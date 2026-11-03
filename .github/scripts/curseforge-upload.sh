#!/usr/bin/env bash
# Uploads every jar in the directory $1 to the CurseForge project $PROJECT_ID as release $TAG.
#
# The jars are named `<mod id>-<loader>-<version>+<node>.jar`. A jar supports the Minecraft versions in the
# `mod.mc_compat` range of its Fabric node in stonecutter.properties.toml (both loaders of a node share the range),
# resolved against the release versions CurseForge knows. The changelog is the section of the version in CHANGELOG.md.
# ENVIRONMENTS names the sides the mod runs on, `Client Server` by default.
# With DRY_RUN=1 the files are only listed with their metadata.
set -euo pipefail

api=https://minecraft.curseforge.com/api
version=${TAG#v}
: "${CURSEFORGE_TOKEN:?the CURSEFORGE_TOKEN secret is not set}"
: "${PROJECT_ID:?the CURSEFORGE_PROJECT_ID variable is not set}"

curseforge() {
    curl --fail-with-body -sS -H "X-Api-Token: $CURSEFORGE_TOKEN" "$@"
}

# version_le A B: whether version A is lower than or equal to version B
version_le() {
    [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | head -n 1)" = "$1" ]
}

# in_range VERSION CONDITION...: whether VERSION meets every Fabric style condition (`>=1.21`, `<26.2-`, `1.21.5`)
in_range() {
    local version=$1 condition bound
    shift
    for condition in "$@"; do
        # a trailing `-` only includes pre-releases, CurseForge files are for releases
        condition=${condition%-}
        bound=${condition#[<>=]}
        bound=${bound#=}
        case $condition in
            '>='*) version_le "$bound" "$version" || return 1 ;;
            '<='*) version_le "$version" "$bound" || return 1 ;;
            '>'*) { version_le "$bound" "$version" && [ "$version" != "$bound" ]; } || return 1 ;;
            '<'*) { version_le "$version" "$bound" && [ "$version" != "$bound" ]; } || return 1 ;;
            *) [ "$version" = "$condition" ] || return 1 ;;
        esac
    done
}

property() {
    sed -n -E "s/^$1 = \"(.*)\"$/\\1/p" stonecutter.properties.toml | head -n 1
}

changelog=$(awk -v head="## [$version]" 'index($0, head) == 1 { found = 1; next } found && /^## \[/ { exit } found' CHANGELOG.md)
if [ -z "$(tr -d '[:space:]' <<<"$changelog")" ]; then
    echo "CHANGELOG.md has no section for $version" >&2
    exit 1
fi
mod_name=$(property 'mod\.name')

types=$(curseforge "$api/game/version-types")
versions=$(curseforge "$api/game/versions")
# release versions of Minecraft, without snapshots
minecraft=$(jq -r --argjson types "$types" '
    [$types[] | select(.slug | test("^minecraft-[0-9]")) | .id] as $ids
    | .[] | select(.gameVersionTypeID as $type | $ids | index($type)) | select(.name | test("^[0-9]+(\\.[0-9]+)+$"))
    | "\(.id) \(.name)"' <<<"$versions")
# version_id TYPE NAME: the id of the game version NAME of the version type with the slug TYPE
version_id() {
    jq -r --arg slug "$1" --arg name "$2" --argjson types "$types" '
        [$types[] | select(.slug == $slug) | .id] as $ids
        | first(.[] | select((.gameVersionTypeID as $type | $ids | index($type)) and .name == $name) | .id) // empty' <<<"$versions"
}

environments=()
for environment in ${ENVIRONMENTS:-Client Server}; do
    id=$(version_id environment "$environment")
    if [ -z "$id" ]; then
        echo "CurseForge knows no environment $environment" >&2
        exit 1
    fi
    environments+=("$id")
done

for jar in "$1"/*.jar; do
    name=$(basename "$jar" .jar)
    node=${name##*+}
    loader=${name#*-}
    loader=${loader%%-*}
    case $loader in
        fabric)
            loader_name=Fabric
            relations='[{"slug": "fabric-api", "type": "requiredDependency"}, {"slug": "modmenu", "type": "optionalDependency"}]'
            ;;
        neoforge)
            loader_name=NeoForge
            relations='[]'
            ;;
        *) echo "$jar: unknown loader $loader" >&2; exit 1 ;;
    esac

    range=$(awk -v section="[fabric.\"$node\"]" '
        $0 == section { found = 1; next }
        found && /^\[/ { exit }
        found && /^mod\.mc_compat/ { sub(/^[^"]*"/, ""); sub(/"$/, ""); print; exit }' stonecutter.properties.toml)
    read -ra conditions <<<"$range"
    ids=()
    names=()
    while read -r id mc; do
        if in_range "$mc" "${conditions[@]}"; then
            ids+=("$id")
            names+=("$mc")
        fi
    done <<<"$minecraft"
    if [ ${#ids[@]} -eq 0 ]; then
        echo "$jar: CurseForge knows no Minecraft version in \"$range\"" >&2
        exit 1
    fi
    loader_game_version=$(version_id modloader "$loader_name")
    if [ -z "$loader_game_version" ]; then
        echo "CurseForge knows no loader $loader_name" >&2
        exit 1
    fi
    ids+=("$loader_game_version" "${environments[@]}")

    metadata=$(jq -n \
        --arg changelog "$changelog" \
        --arg displayName "$mod_name $version for $loader_name $node" \
        --argjson gameVersions "$(printf '%s\n' "${ids[@]}" | jq -s 'map(tonumber)')" \
        --argjson relations "$relations" \
        '{changelog: $changelog, changelogType: "markdown", displayName: $displayName, gameVersions: $gameVersions,
          releaseType: "release"}
         # CurseForge rejects an empty list of related projects
         + if $relations == [] then {} else {relations: {projects: $relations}} end')
    echo "$(basename "$jar"): $loader_name, Minecraft ${names[*]}, ${ENVIRONMENTS:-Client Server}"
    if [ "${DRY_RUN:-}" = 1 ]; then
        echo "$metadata"
        continue
    fi
    # --form-string: the changelog may contain characters that -F would interpret
    curseforge --form-string "metadata=$metadata" -F "file=@$jar" "$api/projects/$PROJECT_ID/upload-file"
    echo
done

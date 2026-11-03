# Notes for Claude

## Changelog

`CHANGELOG.md` follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and must be updated for every
release:

- Add a `## [x.y.z] - YYYY-MM-DD` section at the top with `### Added` / `### Changed` / `### Fixed` /
  `### Removed`, describing the changes from a player's point of view, not commit by commit.
- Add the compare link at the bottom: `[x.y.z]: https://github.com/welles/minecraft-elytra-buff/compare/vPREVIOUS...vx.y.z`.
- Commit the changelog before creating the release.

## CurseForge description

`CURSEFORGE_DESCRIPTION.md` is the text of the mod's CurseForge page, pasted there by hand. Keep it up to date: whenever
a change affects what players see (features, defaults, installation, requirements, supported versions), update it in
the same commit as the README and tell the user to paste it into CurseForge.

- The front matter holds the `summary`, the text CurseForge shows in its lists: catchy, for players, at most 256
  characters, with the hook in the first sentence. Everything below the front matter is the description.
- The description must always start with the prominent AI disclaimer (code, description and icon are AI-generated,
  why the author uses generative AI). Keep its wording the same in every mod.
- Write it for players: what the mod does, how to install it, what can be configured; no build instructions.
- Keep every paragraph and list item on a single line, since the CurseForge editor may turn line breaks into breaks.

## Recordings

The GIFs in `media/` show the mod in action and are embedded in the README and the CurseForge description. They are
recorded by the client game test in `src/gametest/` (only on the 1.21.11-fabric node, not part of the mod jar), which
plays each scene in a headless Minecraft and saves every tick as a frame:

    nix-shell --run scripts/record-gifs.sh

Record them again and check a few frames whenever a change affects what the scenes show (wing animation, particles,
launch height, flight). Taking a screenshot advances the game by one tick, so one frame is one tick.

Every GIF must be at most 2 MB (2,000,000 bytes), CurseForge does not accept larger ones. The script picks the best
frame rate and palette that fit and fails otherwise; then shorten the scene or calm it down (a plain floor, few
camera moves) rather than lowering the limit.

## Releases

1. Check that the `Build` workflow run of the latest commit on `main` is green. If a node fails while resolving
   dependencies (e.g. a POM from libraries.minecraft.net) although it builds locally, the GitHub Actions cache is
   probably broken: `gh cache delete --all -R welles/minecraft-elytra-buff`, then rerun the failed jobs.
2. Create the release with the changelog section as notes, followed by
   `**Full Changelog**: https://github.com/welles/minecraft-elytra-buff/compare/vPREVIOUS...vx.y.z`:
   `gh release create vx.y.z -R welles/minecraft-elytra-buff --target main --title x.y.z --notes-file <notes>`
3. Publishing the release runs `release.yml`, which builds every node with the version from the tag and attaches
   the jars. Check that all of them are attached. `mod.version` in the repo stays `0.0.0-dev`.
4. After attaching the jars, `release.yml` calls `curseforge.yml`, which uploads them to CurseForge with the changelog
   section, if the repository variable `CURSEFORGE_PROJECT_ID` and the secret `CURSEFORGE_TOKEN` are set. It can also
   be started by hand for an existing release: `gh workflow run curseforge.yml -R welles/minecraft-elytra-buff -f tag=vx.y.z`.
   Every run uploads new files, so do not run it twice for the same release. To finish an interrupted upload, upload
   only the missing jars with `-f files=<pattern>`, e.g. `-f 'files=*-neoforge-*'`. The repository variable
   `CURSEFORGE_ENVIRONMENTS` (default `Client Server`) sets the sides the mod runs on.

New features are a minor release, fixes only a patch release.

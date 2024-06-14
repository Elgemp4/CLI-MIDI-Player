# CLI-MIDI-Player

CLI-MIDI-Player is a command-line interface (CLI) program that allows you to play MIDI files easily from your terminal. With features to list available MIDI files, play MIDI files by index or name, specify a soundbank, and reset the playback, CLI-MIDI-Player provides a straightforward and efficient way to enjoy your MIDI music.

## Features

- List available MIDI files
- Play MIDI files by index
- Play MIDI files by name
- Specify a soundbank for playback
- Reset the playback method

## Usage

```bash
cli-midi-player.exe [OPTIONS] [MIDI_FILES...]
```

### Options

- `-l` : List all available MIDI files.
- `-p <index>` : Play the MIDI file by the given index.
- `-P <name>` : Play the MIDI file by the given name.
- `-s <soundbank>` : Specify the soundbank to be used for playback.
- `-r` : Reset the playback method.

### Arguments

- `MIDI_FILES` : List of MIDI files to play.

### Examples

#### List all available MIDI files
```bash
cli-midi-player.exe -l
```

#### Play a MIDI file by index
```bash
cli-midi-player.exe -p 1 example.mid
```

#### Play a MIDI file by name
```bash
cli-midi-player.exe -P "Unknow vendor USB MIDI Interface 2.84" example.mid
```

#### Specify a soundbank for playback
```bash
cli-midi-player.exe -s soundbank.sf2
```

#### Reset the playback method
```bash
cli-midi-player.exe -r
```

#### Play a list of MIDI files
```bash
cli-midi-player.exe file1.mid file2.mid file3.mid
```

## Installation

1. Download the latest release from the [releases page](https://github.com/yourusername/cli-midi-player/releases).
2. Extract the downloaded archive.
3. Move the executable to a directory in your PATH.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your changes. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to [tomari](https://github.com/tomari) the original coder of the project : [Old Project](https://github.com/tomari/PlaySMF)

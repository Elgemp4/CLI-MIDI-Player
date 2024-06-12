package be.elgem.midi;

import org.apache.commons.cli.*;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.io.IOException;

public class Main {
    private int outPortID = -1;
    private String resetType = null;
    private String soundBankPath = null;
    private CommandLine line;

    public static void main(String[] args) {
        new Main(args);
    }
    public Main(String[] args){
        Options options = buildOptions();

        try {
            parseParamaters(args, options);
            playSMFFiles(line.getArgs());
        } catch (ParseException e) {
            System.err.println("Parsing failed. Reason: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PlaySMF", options);
            System.exit(1);
        } catch (MidiUnavailableException e) {
            System.err.println("MIDI device unavailable. The device may be in use by another application.");
            System.exit(1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("The MIDI device with index \""+ outPortID  + "\" does not exist. Use -l to list available devices.");
            System.exit(1);
        }

        System.exit(0);
    }

    public void parseParamaters(String[] args, Options options) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        this.line = parser.parse(options, args);

        // List MIDI devices
        if (line.hasOption('l'))
            fetchMidiDevices(line.hasOption('a'));

        // Get output MIDI port number
        if (line.hasOption('p'))
            this.outPortID = line.getParsedOptionValue("p");

        // Get SoundFont path
        if (line.hasOption('s'))
            this.soundBankPath = line.getParsedOptionValue("s");

        // Get Reset Type
        if (line.hasOption('r'))
            this.resetType = line.getParsedOptionValue("r");
    }

    public Options buildOptions()
    {
        Options options = new Options();
        options.addOption("l", false, "List MIDI devices");
        options.addOption("a", false, "when supplied with -l, list all MIDI devices");
        options.addOption(Option.builder("p").hasArg(true).type(Integer.class)
                .desc("Output port (index shown with -l option)").build());
        options.addOption(Option.builder("s").hasArg(true).type(String.class)
                .desc("Soundfont (path to .sf2 file)").build());
        options.addOption(Option.builder("r").hasArg(true).type(String.class)
                .desc("Reset method: one of (gm gs sc88 xg mu100 doc mt32 fb01)").build());

        return options;
    }

    public void fetchMidiDevices(boolean dumpAll) {
        MidiDevice.Info[] devInfo = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < devInfo.length; i++) {
            MidiDevice.Info nfo = devInfo[i];
            printMidiDevice(nfo, i, dumpAll);
        }
    }

    private void printMidiDevice(MidiDevice.Info nfo, int index, boolean dumpAll){
        MidiDevice device;
        try {
            device = MidiSystem.getMidiDevice(nfo);
        } catch (MidiUnavailableException e) {
            System.out.println("Dev " + index + " unavailable");
            return;
        }

        int maxRx = device.getMaxReceivers();
        if (dumpAll || maxRx != 0) {
            System.out.println("Dev " + index + " " + nfo.getVendor() + " " + nfo.getName() + " " + nfo.getVersion());
            System.out.println("    " + nfo.getDescription());
        }
    }

    private void playSMFFiles(String[] smfFiles) throws ArrayIndexOutOfBoundsException, MidiUnavailableException {
        MidiPlayer midiPlayer = new MidiPlayer(this.outPortID, resetType);

        try {
            midiPlayer.prepare(this.soundBankPath);
        } catch (IOException | InvalidMidiDataException e){
            System.err.println("Incorrect SoundFont file path");
        }

        System.out.println("Playing " + smfFiles.length + " file" + (smfFiles.length > 1 ? "s" : "") + "...");
        for (String smf : smfFiles) {
            System.out.println("Playing " + smf + "...");
            try {
                midiPlayer.play(smf);
            } catch (InvalidMidiDataException | IOException e) {
                System.err.println("The file : " + smf + " is not a valid midi file !");
            }
        }
        midiPlayer.close();
    }
}

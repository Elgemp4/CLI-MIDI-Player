package be.elgem.midi;

import org.apache.commons.cli.*;

import javax.naming.InvalidNameException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.io.IOException;
import java.util.List;

public class Main {
    private int outPortID = -1;
    private String resetType = null;
    private String soundBankPath = null;
    private CommandLine line;

    private MidiPlayer midiPlayer;

    public static void main(String[] args) {
        new Main(args);
    }
    public Main(String[] args){
        handlePrematureShutdown();
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

    private void handlePrematureShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(this.midiPlayer != null && this.midiPlayer.isRunning()){
                    this.midiPlayer.stopAllOscillators();
                }
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
        }));
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

        if(line.hasOption('P')) {
            try {
                this.outPortID = this.getDeviceID(line.getParsedOptionValue("P"));
            } catch (InvalidNameException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

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
        options.addOption(Option.builder("P").hasArg(true).type(String.class)
                .desc("Output device name (name shown with -l option)").build());
        options.addOption(Option.builder("s").hasArg(true).type(String.class)
                .desc("Soundfont (path to .sf2 file)").build());
        options.addOption(Option.builder("r").hasArg(true).type(String.class)
                .desc("Reset method: one of (gm gs sc88 xg mu100 doc mt32 fb01)").build());

        return options;
    }

    public void fetchMidiDevices(boolean dumpAll) {
        List<MidiDevice> devices = getMidiDevices();
        for (int i = 0; i < devices.size(); i++) {
            printMidiDevice(devices.get(i), i, dumpAll);
        }
    }

    private List<MidiDevice> getMidiDevices() {
        MidiDevice.Info[] devInfo = MidiSystem.getMidiDeviceInfo();
        List<MidiDevice> devices = new java.util.ArrayList<>();

        for (MidiDevice.Info nfo : devInfo) {
            try{
                MidiDevice device = MidiSystem.getMidiDevice(nfo);

                devices.add(device);
            } catch (MidiUnavailableException ignored){}
        }

        return devices;
    }

    private String getDeviceFullName(MidiDevice.Info nfo){
        return nfo.getVendor() + " " + nfo.getName() + " " + nfo.getVersion();
    }

    private void printMidiDevice(MidiDevice device, int index, boolean dumpAll){
        MidiDevice.Info nfo = device.getDeviceInfo();

        int maxRx = device.getMaxReceivers();
        if (dumpAll || maxRx != 0) {
            System.out.println("Dev " + index + " " + getDeviceFullName(nfo));
            System.out.println("    " + nfo.getDescription());
        }
    }

    private void playSMFFiles(String[] smfFiles) throws ArrayIndexOutOfBoundsException, MidiUnavailableException {
        this.midiPlayer = new MidiPlayer(this.outPortID, resetType);

        try {
            midiPlayer.prepare(this.soundBankPath);
        } catch (IOException | InvalidMidiDataException e){
            System.err.println("Incorrect SoundFont file path");
        }

        System.out.println("Playing " + smfFiles.length + " file" + (smfFiles.length > 1 ? "s" : "") + "...");
        for (String smf : smfFiles) {
            System.out.println("Playing " + smf + "...");
            try {
                midiPlayer.stopAllOscillators();
                midiPlayer.play(smf);
            } catch (InvalidMidiDataException | IOException e) {
                System.err.println("The file : " + smf + " is not a valid midi file !");
            }
        }
        midiPlayer.close();
    }

    private int getDeviceID(String deviceName) throws InvalidNameException {
        List<MidiDevice> devices = getMidiDevices();
        int index = -1;

        for (int i = 0; i < devices.size(); i++) {
            MidiDevice device = devices.get(i);
            MidiDevice.Info nfo = device.getDeviceInfo();

            if(device.getMaxReceivers() == 0) continue;

            if (getDeviceFullName(nfo).contains(deviceName)) {
                if(index != -1)
                    throw new InvalidNameException(deviceName + " is ambiguous. Please provide a more specific name.");

                index = i;
            }
        }

        if(index != -1)
            return index;

        throw new InvalidNameException("The MIDI device with name \""+ deviceName  + "\" does not exist. Use -l to list available devices.");
    }
}

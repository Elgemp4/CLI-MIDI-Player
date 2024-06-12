package be.elgem.midi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.sound.midi.*;

public class MidiPlayer implements Closeable {
    private static final int UNDEFINED = -1;

    private static final String help =
            "PlaySMF Version 3\n" +
                    "PlaySMF [-p dev] [-r gm|gs|sc88|xg|mu100|doc|mt32|fb01] [-s soundfont.sf2] [-l|-la] 1.mid 2.mid 3.mid ...";
    public static final byte[][] RESET_GM = {{(byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7}};
    public static final byte[][] RESET_GS = {
            {(byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7}, // GM
            {(byte) 0xF0, 0x41, 0x20, 0x42, 0x12, 0x40, 0x00, 0x7f, 0x00, 0x41, (byte) 0xf7}}; // GS
    public static final byte[][] RESET_XG = {
            {(byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7}, // GM
            {(byte) 0xF0, 0x43, 0x10, 0x4c, 0x00, 0x00, 0x7e, 0x00, (byte) 0xf7}, // XG
            {(byte) 0xf0, 0x43, 0x10, 0x49, 0x00, 0x00, 0x12, 0x00, (byte) 0xf7}}; // MU Basic MAP
    public static final byte[][] RESET_MU100 = {
            {(byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7}, // GM
            {(byte) 0xF0, 0x43, 0x10, 0x4c, 0x00, 0x00, 0x7e, 0x00, (byte) 0xf7}, // XG
            {(byte) 0xf0, 0x43, 0x10, 0x49, 0x00, 0x00, 0x12, 0x01, (byte) 0xf7}}; // MU Native MAP
    public static final byte[][] RESET_DOC = {
            {(byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7}, // GM
            {(byte) 0xF0, 0x43, 0x73, 0x01, 0x14, (byte) 0xf7}}; // Disk Orchestra
    public static final byte[][] RESET_MT32 = {
            {(byte) 0xF0, 0x41, 0x10, 0x16, 0x11, 0x7f, 0x00, 0x00, 0x01, 0x00, (byte) 0xf7}}; // MT-32 Reset all
    public static final byte[][] RESET_FB01 = {
            {(byte) 0xF0, 0x43, 0x75, 0x00, 0x20, 0x40, 0x11, (byte) 0xf7}}; // Store configuration
    public static final byte[][] RESET_SC88 = {
            {(byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7}, // GM
            {(byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x00, 0x00, 0x7F, 0x00, 0x01, (byte) 0xF7}}; // System mode set mode-1
    private MidiDevice outputDevice;
    private final int outputDeviceIndex;
    private final byte[][] resetSequence;
    private Receiver midiOutput;
    private Sequencer sequencer;

    protected MidiPlayer(int outputDeviceIndex, String resetSeqLabel) {
        this.outputDeviceIndex = outputDeviceIndex;

        if (resetSeqLabel == null) {
            resetSequence = RESET_GM;
        } else {
            resetSequence = chooseResetSequenceByName(resetSeqLabel);
        }
    }

    public void prepare(String soundBankPath) throws MidiUnavailableException, IOException, InvalidMidiDataException, ArrayIndexOutOfBoundsException {
        openMidiDevice();

        loadSoundBank(soundBankPath);

        createSequencer();
    }

    private void openMidiDevice() throws MidiUnavailableException, ArrayIndexOutOfBoundsException {
        if (outputDeviceIndex == UNDEFINED) {
            outputDevice = MidiSystem.getSynthesizer();
        } else {
            MidiDevice.Info[] devInfo = MidiSystem.getMidiDeviceInfo();
            outputDevice = MidiSystem.getMidiDevice(devInfo[outputDeviceIndex]);
        }

        outputDevice.open();
    }

    private void loadSoundBank(String soundBankPath) throws IOException, InvalidMidiDataException {
        if (soundBankPath != null && outputDevice instanceof Synthesizer synth) {
            Soundbank sb = MidiSystem.getSoundbank(new File(soundBankPath));
            synth.loadAllInstruments(sb);
        }
    }

    private void createSequencer() throws MidiUnavailableException {
        sequencer = MidiSystem.getSequencer();

        detachExistingTransmitters();

        connectSequencerToDevice();

        sequencer.open();
    }

    private void detachExistingTransmitters() {
        java.util.List<Transmitter> existingTransmitters = sequencer.getTransmitters();
        existingTransmitters.forEach(Transmitter::close);
    }

    private void connectSequencerToDevice() throws MidiUnavailableException {
        Transmitter seqTx = sequencer.getTransmitter();
        this.midiOutput = outputDevice.getReceiver();
        seqTx.setReceiver(this.midiOutput);
    }

    public void play(String path) throws InvalidMidiDataException, IOException {
        File midFile = new File(path);
        Sequence musicSequence = MidiSystem.getSequence(midFile);

        sendReset();

        sequencer.setSequence(musicSequence);

        sequencer.start();

        while (sequencer.isRunning()) {}
        sequencer.stop();

    }

    @Override
    public void close() {
        if(midiOutput != null) {
            sendReset();
            midiOutput.close();
        }

        if(sequencer != null)
            sequencer.close();

        if(outputDevice != null)
            outputDevice.close();
    }

    private void sendReset() {
        for (byte[] sysex : resetSequence) {
            SysexMessage sm;
            try {
                sm = new SysexMessage(sysex, sysex.length);
                midiOutput.send(sm, -1);
                Thread.sleep(50);
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
                return;
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void stopAllOscillators() throws InvalidMidiDataException {
        if(midiOutput == null)
            return;

        ShortMessage allNoteOffMessage = new ShortMessage();

        for(int i = 0; i < 16; i++){
            allNoteOffMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 123, 0);
            midiOutput.send(allNoteOffMessage, -1);
            allNoteOffMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 120, 0);
            midiOutput.send(allNoteOffMessage, -1);
        }
    }

    public static byte[][] chooseResetSequenceByName(String seqName) {
        if (seqName.equalsIgnoreCase("gm")) {
            return RESET_GM;
        } else if (seqName.equalsIgnoreCase("gs")) {
            return RESET_GS;
        } else if (seqName.equalsIgnoreCase("xg")) {
            return RESET_XG;
        } else if (seqName.equalsIgnoreCase("mu100")) {
            return RESET_MU100;
        } else if (seqName.equalsIgnoreCase("doc")) {
            return RESET_DOC;
        } else if (seqName.equalsIgnoreCase("mt32")) {
            return RESET_MT32;
        } else if (seqName.equalsIgnoreCase("fb01")) {
            return RESET_FB01;
        } else if (seqName.equalsIgnoreCase("sc88")) {
            return RESET_SC88;
        } else {
            throw new RuntimeException("Unknown reset sequence specified");
        }
    }

    public boolean isRunning() {
        return sequencer.isRunning();
    }
}

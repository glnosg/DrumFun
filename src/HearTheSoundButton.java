import javax.sound.midi.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HearTheSoundButton {

    JButton button;
    private int drumNumber;
    private String buttonLabel;

    public HearTheSoundButton(int nr, String lbl) {
        setDrumNumber(nr);
        setButtonLabel(lbl);
    }

    public void setDrumNumber(int nr) {
        drumNumber = nr;
    }

    public void setButtonLabel(String lbl) {
        buttonLabel = lbl;
    }

    public void createTheButton() {
        button = new JButton(buttonLabel);
        button.addActionListener(new MyButtonListener());
    }

    public void play() {

        try {

            Sequencer player = MidiSystem.getSequencer();
            player.open();

            Sequence seq = new Sequence(Sequence.PPQ, 4);

            Track track = seq.createTrack();

            ShortMessage inst = new ShortMessage();
            inst.setMessage(192, 9, 127, 1);
            MidiEvent instChange = new MidiEvent(inst, 1);
            track.add(instChange);

            ShortMessage a = new ShortMessage();
            a.setMessage(144, 9, drumNumber, 100);
            MidiEvent noteOn = new MidiEvent(a, 1);
            track.add(noteOn);

            ShortMessage b = new ShortMessage();
            b.setMessage(128, 9, drumNumber, 100);
            MidiEvent noteOff = new MidiEvent(b, 2);
            track.add(noteOff);

            player.setSequence(seq);

            player.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public class MyButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            play();
        }
    }
}


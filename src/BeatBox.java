import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

public class BeatBox {

    private String userName;

    JPanel mainPanel;
    JFrame theFrame;
    JTextField tempoValueField;
    JSlider beatsPerMinute;
    JPanel hearTheSoundButtonPanel;
    JList incomeList;

    Sequencer sequencer;
    Sequence sequence;
    Track track;

    ArrayList<JCheckBox> checkBoxList;
    ArrayList<HearTheSoundButton> hearTheSoundButtonList;
    Vector<String> listVector = new Vector<String>();
    HashMap<String, boolean[]> patternsFromServer = new HashMap<String, boolean[]>();

    ObjectInputStream in;
    ObjectOutputStream out;

    static final int BPM_MIN = 0;
    static final int BPM_MAX = 320;
    static final int BPM_INIT = 120;
    private float currentTempo = 120;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat",
        "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
        "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga",
        "Cowbell", "Vibraslap", "Low-Mid Tom", "High Agogo",
        "Open Hi Conga"};
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        new BeatBox().setUp();
    }

    public void setUp() {
        buildGUI();
        String randomUsers[] = {"Pawel", "Kasia", "Wojtek", "Krzys", "Karolina", "Ola"};
        userName = randomUsers[(int) (Math.random() * 6)];

        try {
            Socket socket = new Socket("127.0.0.1", 47017);

            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());

            Thread listener = new Thread(new ServerListener());
            listener.start();

            System.out.println("Connection established");

        } catch (Exception ex) {
            String[] e = {"No Connection"};
            incomeList.setListData(e);
            ex.printStackTrace();
        }
    }

    public void buildGUI() {
        theFrame = new JFrame("Drum Pattern");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        checkBoxList = new ArrayList<JCheckBox>();
        hearTheSoundButtonList = new ArrayList<HearTheSoundButton>();
        GridLayout functButtonGrid = new GridLayout(7, 1);
        JPanel functButtonPanel = new JPanel(functButtonGrid);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        functButtonPanel.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        functButtonPanel.add(stop);

        JButton upTempo = new JButton("Tempo x 1.03");
        upTempo.addActionListener(new MyUpTempoListener());
        functButtonPanel.add(upTempo);

        JButton downTempo = new JButton("Tempo x 0.97");
        downTempo.addActionListener(new MyDownTempoListener());
        functButtonPanel.add(downTempo);

        JButton savePattern = new JButton("Save Pattern");
        savePattern.addActionListener(new MySavePatternListener());
        functButtonPanel.add(savePattern);

        JButton loadPattern = new JButton("Load Pattern");
        loadPattern.addActionListener(new MyLoadPatternListener());
        functButtonPanel.add(loadPattern);

        JButton sendPattern = new JButton("Send Pattern");
        sendPattern.addActionListener(new MySendPatternListener());
        functButtonPanel.add(sendPattern);

        incomeList = new JList();
        incomeList.addListSelectionListener(new MyListSelectionListener());
        incomeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomeList);
        theList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        theList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        incomeList.setListData(listVector);

        JPanel functionPanel = new JPanel(new GridLayout(2, 1));
        functionPanel.add(functButtonPanel);
        functionPanel.add(theList);

        tempoValueField = new JTextField(8);
        tempoValueField.setText("120");
        String sliderDescription = "Current tempo: ";

        JPanel sliderPanel = new JPanel();
        sliderPanel.add(new Label(sliderDescription));
        sliderPanel.add(tempoValueField);

        beatsPerMinute = new JSlider(JSlider.HORIZONTAL,
                                                BPM_MIN, BPM_MAX, BPM_INIT);
        beatsPerMinute.addChangeListener(new MySliderListener());
        beatsPerMinute.setMajorTickSpacing(10);
        beatsPerMinute.setMinorTickSpacing(1);
        beatsPerMinute.setPaintTicks(true);
        sliderPanel.add(beatsPerMinute);

        GridLayout hearTheSoundButtonGrid = new GridLayout(16, 1);
        hearTheSoundButtonGrid.setVgap(1);
        hearTheSoundButtonPanel = new JPanel(hearTheSoundButtonGrid);

        for (int i = 0; i < 16; i++) {
            hearTheSoundButtonList.add(new HearTheSoundButton(instruments[i], instrumentNames[i]));
            hearTheSoundButtonList.get(i).createTheButton();
            hearTheSoundButtonPanel.add(hearTheSoundButtonList.get(i).button);
        }

        background.add(BorderLayout.EAST, functionPanel);
        background.add(BorderLayout.WEST, hearTheSoundButtonPanel);
        background.add(BorderLayout.SOUTH, sliderPanel);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();

        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void buildTrackAndStart() {
        int[] trackList = null;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++) {
            trackList = new int[16];

            int key = instruments[i];

            for (int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) checkBoxList.get(j + (16 * i));
                if (jc.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                }
            }
            makeTracks(trackList);
            track.add(makeEvent(176, 1, 127, 0, 16));
        }

        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void makeTracks(int[] list) {

        for (int i = 0; i < 16; i++) {
            int key = list[i];

            if (key != 0) {
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }

    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            if (tempoFactor < 2.66f)
                sequencer.setTempoFactor((float) (tempoFactor * 1.03));
            beatsPerMinute.setValue((int) (sequencer.getTempoFactor() * 120));
            tempoValueField.setText(Integer.toString((int)(sequencer.getTempoFactor() * 120)));

        }
    }

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            if (tempoFactor > 0)
                sequencer.setTempoFactor((float) (tempoFactor * 0.97));
            beatsPerMinute.setValue((int) (sequencer.getTempoFactor() * 120));
            tempoValueField.setText(Integer.toString((int)(sequencer.getTempoFactor() * 120)));
        }
    }

    public class MySavePatternListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkBoxState = new boolean[256];

            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkBoxList.get(i);
                if (check.isSelected()) {
                    checkBoxState[i] = true;
                }
            }

            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(theFrame);

            try {
                FileOutputStream fileStream = new FileOutputStream(fileSave.getSelectedFile());
                ObjectOutputStream os = new ObjectOutputStream(fileStream);
                os.writeObject(checkBoxState);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class MyLoadPatternListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkBoxState = null;

            JFileChooser fileOpen = new JFileChooser();
            fileOpen.showOpenDialog(theFrame);

            try {
                FileInputStream fileIn = new FileInputStream(fileOpen.getSelectedFile());
                ObjectInputStream is= new ObjectInputStream(fileIn);
                checkBoxState = (boolean[]) is.readObject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkBoxList.get(i);
                if(checkBoxState[i]) {
                    check.setSelected(true);
                } else {
                    check.setSelected(false);
                }
            }

            sequencer.stop();
            //buildTrackAndStart();
        }
    }

    public class MySendPatternListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {
            boolean[] checkBoxState = new boolean[256];

            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkBoxList.get(i);

                if (check.isSelected()) {
                    checkBoxState[i] = true;
                }
            }
            try {
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                Date date = new Date();

                out.writeObject(userName + "[" + dateFormat.format(date) + "]: New Pattern");
                out.writeObject(checkBoxState);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class MySliderListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            currentTempo = (int)source.getValue();
            float tempoFactor = ((float) (currentTempo / 120));
            sequencer.setTempoFactor(tempoFactor);
            tempoValueField.setText(Integer.toString((int)(sequencer.getTempoFactor() * 120)));
        }
    }

    public class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent ev) {

            String selected = (String) incomeList.getSelectedValue();
            boolean[] checkBoxState = (boolean[]) patternsFromServer.get(selected);

            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkBoxList.get(i);
                if(checkBoxState[i]) {
                    check.setSelected(true);
                } else {
                    check.setSelected(false);
                }
            }

            sequencer.stop();
            //buildTrackAndStart();
        }
    }

    public class ServerListener implements Runnable {
        Object obj;
        public void run() {
            try {
                while((obj = in.readObject()) != null) {
                    String keyName = (String) obj;
                    boolean[] checkBoxState = (boolean[]) in.readObject();
                    patternsFromServer.put(keyName, checkBoxState);
                    listVector.add(keyName);
                    incomeList.setListData(listVector);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}



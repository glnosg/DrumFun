import javax.sound.midi.*;

public class MusicTest1 {
	
	public void play() {
		Sequencer sequencer = MidiSystem.getSequencer();
		
		System.out.println("We got sequencer");
	}
	
	public static void main(String[] args) {
		
		MusicTest1 m1 = new MusicTest1();
		m1.play();
	}
}
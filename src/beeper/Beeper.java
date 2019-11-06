package beeper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.Timer;

public class Beeper {
	
	public final static int SINE_WAVE = 1;
	public final static int SQUARE_WAVE = 2;
	public final static int TRIANGLE_WAVE = 3;
	public final static int SAWTOOTH_WAVE = 4;
	
	public static void beep() throws InterruptedException, LineUnavailableException {
		Beeper.beep(800, 200);
	}
	
	public static void beep(double frequency, double duration) throws InterruptedException, LineUnavailableException {
		Beeper.beep(frequency, duration, Beeper.SINE_WAVE);
	}
	
	public static void beep(double frequency, double duration, int type) throws InterruptedException, LineUnavailableException {
		Clip clip = AudioSystem.getClip();
		/**
		 * AudioFormat of reclieved clip. Probably you can alter it
		 * someway choosing proper Line.
		 */
		AudioFormat format = clip.getFormat();
		/**
		 * We assume that encoding uses signed shorts. Probably we could
		 * make this code more generic but who cares.
		 */
		if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
			throw new UnsupportedOperationException("Unknown encoding");
		if (format.getSampleSizeInBits() != 16) {
			throw new UnsupportedOperationException("Weird sample size. Dunno what to do with it.");
		}
		/**
		 * Number of bytes in a single frame
		 */
		int bytesPerFrame = format.getFrameSize();
		/**
		 * Number of frames per second
		 */
		double fps = 11025;
		/**
		 * Number of frames during the clip.
		 */
		int frames = (int) (fps * (duration / 250));
		/**
		 * Data
		 */
		byte[] data = new byte[frames * bytesPerFrame];
		/**
		 * We will emit sinus, which needs to be scaled so it has proper
		 * frequency --- here is the scaling factor.
		 */
		double freqFactor = (Math.PI / 2) * frequency / fps;
		/**
		 * This sinus must also be scaled so it fills short.
		 */
		double ampFactor = Short.MAX_VALUE;
		boolean bigEndian = format.isBigEndian();
		for (int frame = 0; frame < frames; frame++) {
			short sample = (short) 0;
			switch (type) {
				case Beeper.SINE_WAVE:
					sample = (short) (0.8 * ampFactor * Math.sin(frame * freqFactor));
					break;
				case Beeper.SQUARE_WAVE:
					sample = (short) (0.8 * ampFactor * Math.signum(Math.sin(frame * freqFactor)));
					break;
				case Beeper.TRIANGLE_WAVE:
					sample = (short) (0.8 * ampFactor * 0.63 * Math.asin(Math.sin(frame * freqFactor)));
					break;
				case Beeper.SAWTOOTH_WAVE:
					sample = (short) (0.8 * ampFactor * ((frame * freqFactor) / (2 * Math.PI) % 1) * 2 - 0.8 * ampFactor);
					break;
				default:
					throw new IllegalArgumentException("Type can only be Beeper.SINE_WAVE, Beeper.SQUARE_WAVE or Beeper.TRIANGLE_WAVE!");
			}
			data[(frame * bytesPerFrame) + 0] = (byte) (bigEndian ? sample >> 0x08 : sample);
			data[(frame * bytesPerFrame) + 1] = (byte) (bigEndian ? sample : sample >> 0x08);
			data[(frame * bytesPerFrame) + 2] = (byte) (bigEndian ? sample >> 0x08 : sample);
			data[(frame * bytesPerFrame) + 3] = (byte) (bigEndian ? sample : sample >> 0x08);
		}
		clip.open(format, data, 0, data.length);
		// This is so Clip releases its data line when done. Otherwise at 32 Clips it breaks.
		clip.addLineListener(new LineListener() {
			@Override
			public void update(LineEvent e) {
				if (e.getType() == LineEvent.Type.START) {
					Timer t = new Timer((int) Math.round(Math.ceil(duration + 1)), new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							clip.close();
						}
					});
					t.setRepeats(false);
					t.start();
				}
			}
		});
		clip.start();
		Thread.sleep((long) duration);
	}
}

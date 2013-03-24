package ioio.examples.ioio_swing;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.pc.IOIOSwingApp;
import ioio.lib.api.TwiMaster;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

public class IOIOSwing16Servos extends IOIOSwingApp implements ActionListener {
	private static final String BUTTON_PRESSED = "bp";

	// Boilerplate main(). Copy-paste this code into any IOIOapplication.
	public static void main(String[] args) throws Exception {
		new IOIOSwing16Servos().go(args);
	}

	protected boolean ledOn_;

	@Override
	protected Window createMainWindow(String args[]) {
		// Use native look and feel.
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}

		JFrame frame = new JFrame("HelloIOIOSwing");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
		
		JToggleButton button = new JToggleButton("LED");
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setActionCommand(BUTTON_PRESSED);
		button.addActionListener(this);
		contentPane.add(Box.createVerticalGlue());
		contentPane.add(button);
		contentPane.add(Box.createVerticalGlue());

		// Display the window.
		frame.setSize(300, 100);
		frame.setLocationRelativeTo(null); // center it
		frame.setVisible(true);
		
		return frame;
	}

	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		return new BaseIOIOLooper() {
			private DigitalOutput led_;
			
			private final int I2C_PAIR = 0; //IOIO Pair for I2C
			private static final float FREQ = 50.0f;
			private static final int PCA_ADDRESS = 0x40;
			private static final byte PCA9685_MODE1 = 0x00;
			private static final byte PCA9685_PRESCALE = (byte) 0xFE;
			
			private TwiMaster twi_;

			@Override
			protected void setup() throws ConnectionLostException,
					InterruptedException {
				led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
				twi_ = ioio_.openTwiMaster(I2C_PAIR, TwiMaster.Rate.RATE_1MHz, false); // Setup IOIO TWI Pins
				reset();
			}
			
			private void reset() throws ConnectionLostException,
			InterruptedException {
				// Set prescaler - see PCA9685 data sheet
				float prescaleval = 25000000;
				prescaleval /= 4096;
				prescaleval /= FREQ;
				prescaleval -= 1;
				byte prescale = (byte) Math.floor(prescaleval + 0.5);
				
				write8(PCA9685_MODE1, (byte) 0x10); // go to sleep... prerequisite to set prescaler
				write8(PCA9685_PRESCALE, prescale); // set the prescaler
				write8(PCA9685_MODE1, (byte) 0x20); // Wake up and set Auto Increment
			}
			
			void write8(byte reg, byte val) throws ConnectionLostException,
				InterruptedException {
				byte[] request = {reg, val};
				twi_.writeReadAsync(PCA_ADDRESS, false, request, request.length, null, 0);
			}

			@Override
			public void loop() throws ConnectionLostException,
					InterruptedException {
				Thread.sleep(10);
				//All Servos one way
				setServo(0, 0.0f);
				setServo(1, 0.0f);
				setServo(2, 0.0f);
				setServo(3, 0.0f);
				setServo(4, 0.0f);
				setServo(5, 0.0f);
				setServo(6, 0.0f);
				setServo(7, 0.0f);
				setServo(8, 0.0f);
				setServo(9, 0.0f);
				setServo(10, 0.0f);
				setServo(11, 0.0f);
				setServo(12, 0.0f);
				setServo(13, 0.0f);
				setServo(14, 0.0f);
				setServo(15, 0.0f);
				Thread.sleep(1000);
				//All Servos back the other way
				setServo(0, 1.5f);
				setServo(1, 1.5f);
				setServo(2, 1.5f);
				setServo(3, 1.5f);
				setServo(4, 1.5f);
				setServo(5, 1.5f);
				setServo(6, 1.5f);
				setServo(7, 1.5f);
				setServo(8, 1.5f);
				setServo(9, 1.5f);
				setServo(10, 1.5f);
				setServo(11, 1.5f);
				setServo(12, 1.5f);
				setServo(13, 1.5f);
				setServo(14, 1.5f);
				setServo(15, 1.5f);
				Thread.sleep(1000);
				
				//PWM Range below is 0.0. to 1.5.  Cycle through each servo channel.
				for (int c=0; c<16; c++) {
					for (float p = 1.5f; p>0.0; p-=0.1f) {
						Thread.sleep(200);
						setServo(c, p);
						led_.write(ledOn_);
					}
				
					for (float p=0.0f; p<1.5f; p+=0.1f) {
						Thread.sleep(200);
						setServo(c, p);
					}
				}
			}
			
			public void setServo(int servoNum, float pos) throws ConnectionLostException, InterruptedException {
				//Set Servo channel and milliseconds input to PulseWidth calculation
				setPulseWidth(servoNum, pos + 1.0f);  //
			}
			
			public void setPulseWidth(int channel, float ms) throws ConnectionLostException, InterruptedException {
				// Set pulsewidth according to PCA9685 data sheet based on milliseconds value sent from setServo method
				// 4096 steps per cycle, frequency is 50MHz (50 steps per millisecond)
				int pw = Math.round(ms / 1000 * FREQ * 4096);
				// Skip to every 4th address value to turn off the pulse (see datasheet addresses for LED#_OFF_L)
				byte[] request = { (byte) (0x08 + channel * 4), (byte) pw, (byte) (pw >> 8) };
				twi_.writeReadAsync(PCA_ADDRESS, false, request, request.length, null, 0);
			}
		};
	}	
		
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals(BUTTON_PRESSED)) {
			ledOn_ = ((JToggleButton) event.getSource()).isSelected();
		}
	}
}

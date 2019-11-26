package visual.components;

import java.awt.Window;

import javax.swing.JDialog;

import visual.VisualConstants;


public class DDialog extends JDialog {

	private static final long serialVersionUID = -5561626758061177495L;

	public DDialog(Window owner, ModalityType modalityType) {
		super(owner, modalityType);
		
		initializeComponent();
	}
	
	private void initializeComponent() {
		setBackground(VisualConstants.BACK_COLOR);
		this.setResizable(false);
	}
}

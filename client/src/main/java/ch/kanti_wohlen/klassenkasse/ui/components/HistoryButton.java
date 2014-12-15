package ch.kanti_wohlen.klassenkasse.ui.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.gpl.JSplitButton.JSplitButton;
import org.gpl.JSplitButton.action.SplitButtonActionListener;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;

public class HistoryButton extends JSplitButton {

	private final @NonNull Host host;

	private final HistoryListener historyListener;
	private final Deque<BaseAction[]> history;

	/**
	 * Create the panel.
	 */
	@NonNullByDefault
	public HistoryButton(Host host, Deque<BaseAction[]> history, HistoryListener historyListener) {
		this.host = host;
		this.history = history;
		this.historyListener = historyListener;

		setPopupMenu(new HistoryMenu());
		addSplitButtonActionListener(new ButtonActionListener());
	}

	public interface HistoryListener {

		void historyChanged(Collection<BaseAction[]> changedActions);
	}

	// Listener and helper classes

	private class ButtonActionListener implements SplitButtonActionListener {

		final PopUpActionListener popUpListener = new PopUpActionListener();

		@Override
		public void buttonClicked(ActionEvent e) {
			if (history.isEmpty()) return;

			BaseAction[] actions = history.pollLast();
			if (actions == null) return;

			historyListener.historyChanged(Collections.singletonList(actions));
		}

		@Override
		public void splitButtonClicked(ActionEvent e) {
			JPopupMenu menu = getPopupMenu();
			menu.removeAll();

			if (history.isEmpty()) return;

			Iterator<BaseAction[]> iterator = history.descendingIterator();
			while (iterator.hasNext()) {
				BaseAction[] actions = iterator.next();
				if (actions == null || actions.length == 0) continue;

				JMenuItem menuItem = menu.add(Action.getActionDescription(host, actions));
				menuItem.setIcon(IconProvider.forAction(actions[0].getAction()));
				menuItem.setModel(new LazyButtonModel());
				menuItem.addActionListener(popUpListener);
			}

			menu.show(HistoryButton.this, getWidth() - (int) menu.getPreferredSize().getWidth(), getHeight());
		}
	}

	private class PopUpActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			Deque<BaseAction[]> history = HistoryButton.this.history;
			Component item = (Component) e.getSource();
			Component[] components = getPopupMenu().getComponents();
			Collection<BaseAction[]> actions = new ArrayList<>();

			for (Component component : components) {
				actions.add(history.pollLast());

				if (component == item) break;
			}

			historyListener.historyChanged(Collections.unmodifiableCollection(actions));
		}
	}

	private static class HistoryMenu extends JPopupMenu implements MouseListener {

		@Override
		public JMenuItem add(JMenuItem menuItem) {
			JMenuItem add = super.add(menuItem);
			add.addMouseListener(this);
			return add;
		}

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {
			List<Component> components = Arrays.asList(getComponents());

			for (int i = 0; i < components.size(); ++i) {
				Component comp = components.get(i);

				if (comp instanceof JMenuItem) {
					JMenuItem item = (JMenuItem) comp;
					item.setArmed(false);
				}
			}

			repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			List<Component> components = Arrays.asList(getComponents());
			int index = components.indexOf(e.getComponent());

			for (int i = 0; i < components.size(); ++i) {
				Component comp = components.get(i);

				if (comp instanceof JMenuItem) {
					JMenuItem item = (JMenuItem) comp;
					item.setArmed(i <= index);
				}
			}

			repaint();
		}
	}

	/**
	 * A {@link ButtonModel} which does not fire events when {@link #isArmed()} is changed.
	 * This is to reduce visual lag when hovering over items.
	 */
	private static class LazyButtonModel extends DefaultButtonModel {

		@Override
		public void setArmed(boolean b) {
			if (b) {
				stateMask |= ARMED;
			} else {
				stateMask &= ~ARMED;
			}
		}
	}
}

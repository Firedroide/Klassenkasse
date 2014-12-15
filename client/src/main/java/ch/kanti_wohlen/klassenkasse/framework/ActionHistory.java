package ch.kanti_wohlen.klassenkasse.framework;

import java.util.LinkedList;

import ch.kanti_wohlen.klassenkasse.action.BaseAction;

public class ActionHistory {

	public static final int MAX_ITEMS = 20;

	private final LinkedList<BaseAction[]> undoableActions;
	private final LinkedList<BaseAction[]> redoableActions;

	public ActionHistory() {
		undoableActions = new LinkedList<BaseAction[]>();
		redoableActions = new LinkedList<BaseAction[]>();
	}

	public LinkedList<BaseAction[]> getUndoableActions() {
		return undoableActions;
	}

	public LinkedList<BaseAction[]> getRedoableActions() {
		return redoableActions;
	}
}

package ch.kanti_wohlen.klassenkasse.printing;

import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionPrinter implements Printable {

	private final List<Printable> printables;
	private final Map<Class<? extends Graphics>, Integer> printableIndices;
	private final Map<Class<? extends Graphics>, Integer> lastFailedIndices;

	public CollectionPrinter() {
		this(Collections.<Printable>emptyList());
	}

	public CollectionPrinter(Collection<Printable> printables) {
		this.printables = new ArrayList<Printable>(printables);
		printableIndices = new HashMap<>();
		lastFailedIndices = new HashMap<>();
	}

	public void add(Printable element) {
		printables.add(element);
	}

	public void addAll(Collection<Printable> collection) {
		printables.addAll(collection);
	}

	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		Class<? extends Graphics> gClass = graphics.getClass();

		if (!printableIndices.containsKey(gClass)) {
			printableIndices.put(gClass, 0);
		}
		int printableIndex = printableIndices.get(gClass);

		if (!lastFailedIndices.containsKey(gClass)) {
			lastFailedIndices.put(gClass, 0);
		}
		int lastFailedIndex = lastFailedIndices.get(gClass);

		// Get the next printable that can still print
		for (; printableIndex < printables.size(); ++printableIndex) {
			Printable printable = printables.get(printableIndex);
			int localPrinterIndex = pageIndex - lastFailedIndex;

			int result = printable.print(graphics.create(), pageFormat, localPrinterIndex);
			if (result == PAGE_EXISTS) {
				// Great, we printed a page, let's jump out of here
				printableIndices.put(gClass, printableIndex);
				return PAGE_EXISTS;
			} else {
				// This printer could not print any (more) pages
				// So let's reset the index for the next printer to 0
				lastFailedIndex = pageIndex;
				lastFailedIndices.put(gClass, lastFailedIndex);
			}
		}

		// All our printers failed to print a page
		// So we're done printing
		return NO_SUCH_PAGE;
	}
}

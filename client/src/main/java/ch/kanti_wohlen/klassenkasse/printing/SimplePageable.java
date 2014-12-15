package ch.kanti_wohlen.klassenkasse.printing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

public class SimplePageable implements Pageable {

	private final PrinterJob job;
	private final Printable printable;
	private final int pageAmount;

	public SimplePageable(PrinterJob printJob, Printable printable) {
		this.job = printJob;
		this.printable = printable;

		int pageIndex = 0;
		PageFormat format = job.defaultPage();

		try {
			while (printable.print(createPeekGraphics(printJob, pageIndex), format, pageIndex) == Printable.PAGE_EXISTS) {
				++pageIndex;
			}
		} catch (PrinterException e) {
			e.printStackTrace();
		}

		pageAmount = pageIndex;
	}

	@Override
	public int getNumberOfPages() {
		return pageAmount;
	}

	@Override
	public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
		return job.defaultPage();
	}

	@Override
	public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
		return printable;
	}

	/**
	 * Copied from sun.print.RasterPrinterJob, heavily edited
	 */
	private Graphics createPeekGraphics(PrinterJob printJob, int pageIndex) {
		PageFormat page = getPageFormat(pageIndex);

		double xScale = 600 / 72.0; //getXRes() / 72.0;
		double yScale = 600 / 72.0; //getYRes() / 72.0;

		/*
		 * The scale transform is used to switch from the
		 * device space to the user's 72 dpi space.
		 */
		AffineTransform scaleTransform = new AffineTransform();
		scaleTransform.scale(xScale, yScale);

		/*
		 * Create a BufferedImage to hold the band. We set the clip
		 * of the band to be tight around the bits so that the
		 * application can use it to figure what part of the
		 * page needs to be drawn. The clip is never altered in
		 * this method, but we do translate the band's coordinate
		 * system so that the app will see the clip moving down the
		 * page though it s always around the same set of pixels.
		 */
		BufferedImage pBand = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);

		/*
		 * Have the app draw into a PeekGraphics object so we can
		 * learn something about the needs of the print job.
		 */

		Graphics2D peekGraphics = pBand.createGraphics();
		Rectangle2D.Double pageFormatArea =
				new Rectangle2D.Double(page.getImageableX(),
						page.getImageableY(),
						page.getImageableWidth(),
						page.getImageableHeight());
		peekGraphics.transform(scaleTransform);
		peekGraphics.transform(new AffineTransform(page.getMatrix()));
		peekGraphics.setClip(pageFormatArea);

		return peekGraphics;
	}
}

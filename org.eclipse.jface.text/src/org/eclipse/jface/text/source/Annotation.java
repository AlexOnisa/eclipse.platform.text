/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.text.source;



import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

/**
 * Annotation managed by an <code>IAnnotationModel</code>.
 * Annotations are considered being located at layers and are considered being painted
 * starting with layer 0 upwards. Thus an annotation of layer 5 will be drawn on top of
 * all co-located annotations at the layers 4 - 0. Subclasses must provide the annotations
 * paint method.
 *
 * @see IVerticalRuler
 */
public class Annotation {
	
	public final static String TYPE_UNKNOWN= "org.eclipse.text.annotation.unknown";  //$NON-NLS-1$
	
	/**
	 * Convenience method for drawing an image aligned inside a rectangle.
	 *
	 * @param image the image to be drawn
	 * @param gc the drawing GC
	 * @param canvas the canvas on which to draw
	 * @param r the clipping rectangle
	 * @param halign the horizontal alignment of the image to be drawn
	 * @param valign the vertical alignment of the image to be drawn
	 */
	protected static void drawImage(Image image, GC gc, Canvas canvas, Rectangle r, int halign, int valign) {
		if (image != null) {
			
			Rectangle bounds= image.getBounds();
			
			int x= 0;
			switch(halign) {
			case SWT.LEFT:
				break;
			case SWT.CENTER:
				x= (r.width - bounds.width) / 2;
				break;
			case SWT.RIGHT:
				x= r.width - bounds.width;
				break;
			}
			
			int y= 0;
			switch (valign) {
			case SWT.TOP: {
				FontMetrics fontMetrics= gc.getFontMetrics();
				y= (fontMetrics.getHeight() - bounds.height)/2;
				break;
			}
			case SWT.CENTER:
				y= (r.height - bounds.height) / 2;
				break;
			case SWT.BOTTOM: {
				FontMetrics fontMetrics= gc.getFontMetrics();
				y= r.height - (fontMetrics.getHeight() + bounds.height)/2;
				break;
			}
			}
			
			gc.drawImage(image, r.x+x, r.y+y);
		}
	}
	
	/**
	 * Convenience method for drawing an image aligned inside a rectangle.
	 *
	 * @param image the image to be drawn
	 * @param gc the drawing GC
	 * @param canvas the canvas on which to draw
	 * @param r the clipping rectangle
	 * @param align the alignment of the image to be drawn
	 */
	protected static void drawImage(Image image, GC gc, Canvas canvas, Rectangle r, int align) {
		drawImage(image, gc, canvas, r, align, SWT.CENTER);
	}
	
	
	
	/** The layer of this annotation. */
	private int fLayer;
	/**
	 * The type of this annotation.
	 * @since 3.0
	 */
	private String fType;
	/**
	 * Indicates whether this annotation is persistent or not.
	 * @since 3.0
	 */
	private boolean fIsPersistent= false;
	/**
	 * Indicates whether this annotation is marked as deleted or not.
	 * @since 3.0
	 */
	private boolean fMarkedAsDeleted= false;
	/**
	 * The text associated with this annotation.
	 * @since 3.0
	 */
	private String fText;
	
	
	
	/**
	 * Creates a new annotation that is not persistent and type less.
	 */
	protected Annotation() {
		this(null, false, null);
	}
	
	/**
	 * Creates a new annotation with the given properties.
	 * 
	 * @param type the type of this annotation
	 * @param isPersistent <code>true</code> if this annotation is
	 *            persistent, <code>false</code> otherwise
	 * @param text the text associated with this annotation
	 * @since 3.0
	 */
	public Annotation(String type, boolean isPersistent, String text) {
		fType= type;
		fIsPersistent= isPersistent;
		fText= text;
	}
	
	/**
	 * Creates a new annotation.
	 * 
	 * @param isPersistent <code>true</code> if persistent, <code>false</code> otherwise
	 * @since 3.0
	 */
	public Annotation(boolean isPersistent) {
		this(null, isPersistent, null);
	}
	
	/**
	 * Returns whether this annotation is persistent.
	 * 
	 * @return <code>true</code> if this annotation is persistent, <code>false</code>
	 *         otherwise
	 * @since 3.0
	 */
	public boolean isPersistent() {
		return fIsPersistent;
	}
	
	/**
	 * Sets the type of this annotation.
	 * 
	 * @param type the annotation type
	 * @since 3.0
	 */
	public void setType(String type) {
		fType= type;
	}
	
	/**
	 * Returns the type of the annotation.
	 * 
	 * @return the type of the annotation
	 * @since 3.0
	 */
	public String getType() {
		return fType == null? TYPE_UNKNOWN : fType;
	}
	
	/**
	 * Marks this annotation deleted according to the value of the
	 * <code>deleted</code> parameter.
	 * 
	 * @param deleted <code>true</code> if annotation should be marked as deleted
	 * @since 3.0
	 */
	public void markDeleted(boolean deleted) {
		fMarkedAsDeleted= deleted;
	}
	
	/**
	 * Returns whether this annotation is marked as deleted.
	 * 
	 * @return <code>true</code> if annotation is marked as deleted, <code>false</code>
	 *         otherwise
	 * @since 3.0
	 */
	public boolean isMarkedDeleted() {
		return fMarkedAsDeleted;
	}
	
	/**
	 * Sets the text associated with this annotation.
	 * 
	 * @param text the text associated with this annotation
	 * @since 3.0
	 */
	public void setText(String text) {
		fText= text;
	}
	
	/**
	 * Returns the text associated with this annotation.
	 * 
	 * @return the text associated with this annotation or <code>null</code>
	 * @since 3.0
	 */
	public String getText() {
		return fText;
	}
	
	/**
	 * Sets the layer of this annotation.
	 *
	 * @param layer the layer of this annotation
	 * @deprecated since 3.0
	 */
	protected void setLayer(int layer) {
		fLayer= layer;
	}
	
	/**
	 * Returns the annotations drawing layer.
	 *
	 * @return the annotations drawing layer
	 * @deprecated use <code>IAnnotationAccessExtension.getLayer(Annotation)</code>
	 */
	public int getLayer() {
		return fLayer;
	}
	
	/**
	 * Implement this method to draw a graphical representation 
	 * of this annotation within the given bounds. This default implementation
	 * does nothing.
	 *
	 * @param gc the drawing GC
	 * @param canvas the canvas to draw on
	 * @param bounds the bounds inside the canvas to draw on
	 * @deprecated use <code>IAnnotationAccessExtension.paint(Annotation, GC, Canvas, Rectangle)</code>
	 */
	public void paint(GC gc, Canvas canvas, Rectangle bounds) {
	}
}

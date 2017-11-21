/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * - Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.ui.internal.genericeditor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;

/**
*
* This Reconciler Strategy is a default stategy which will be present if
* no other highlightReconcilers are registered for a given content-type. It splits
* the text into 'words' (which are defined as anything in-between
* non-alphanumeric characters) and searches the document highlighting all like words.
*
* E.g. if your file contains "t^he dog in the bog" and you leave your caret at
* ^ you will get both instances of 'the' highlighted.
*
*/
public class DefaultWordHighlightStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension, CaretListener, IPreferenceChangeListener {

	private static final String ANNOTATION_TYPE = "org.eclipse.ui.genericeditor.text"; //$NON-NLS-1$

	private boolean enabled;
	private ISourceViewer sourceViewer;
	private IDocument document;

	private static final String WORD_REGEXP = "[a-zA-Z]+"; //$NON-NLS-1$
	private static final Pattern WORD_PATTERN = Pattern.compile(WORD_REGEXP);

	private Annotation[] fOccurrenceAnnotations = null;

	private void applyHighlights(int offset) {
		if (sourceViewer == null || !enabled) {
			removeOccurrenceAnnotations();
			return;
		}

		String text = document.get();
		offset = ((ITextViewerExtension5)sourceViewer).widgetOffset2ModelOffset(offset);

		int wordStartOffset = findStartingOffset(text, offset);
		int wordEndOffset = findEndingOffset(text, offset);
		if(wordEndOffset <= wordStartOffset || wordEndOffset == -1 || wordStartOffset == -1) {
			removeOccurrenceAnnotations();
			return;
		}
		String word = text.substring(wordStartOffset, wordEndOffset);

		Matcher m = WORD_PATTERN.matcher(text);
		Map<Annotation, Position> annotationMap = new HashMap<>();
		while(m.find()) {
			if(m.group().equals(word)) {
				annotationMap.put(new Annotation(ANNOTATION_TYPE, false, null),
						new Position(m.start(), m.end() - m.start()));
			}
		}

		if(annotationMap.size() < 2) {
			removeOccurrenceAnnotations();
			return;
		}

		IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
		synchronized (getLockObject(annotationModel)) {
			if (annotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension) annotationModel).replaceAnnotations(fOccurrenceAnnotations, annotationMap);
			} else {
				removeOccurrenceAnnotations();
				Iterator<Entry<Annotation, Position>> iter = annotationMap.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<Annotation, Position> mapEntry = iter.next();
					annotationModel.addAnnotation(mapEntry.getKey(), mapEntry.getValue());
				}
			}
			fOccurrenceAnnotations = annotationMap.keySet().toArray(new Annotation[annotationMap.keySet().size()]);
		}
	}

	private static int findStartingOffset(String text, int offset) {
		final Pattern NON_ALPHANUMERIC_LAST_PATTERN = Pattern.compile("[^\\w](?!.*[^\\w])"); //$NON-NLS-1$
		String substring = text.substring(0, offset);
		Matcher m = NON_ALPHANUMERIC_LAST_PATTERN.matcher(substring);
		if(m.find()) {
			return m.end();
		}
		return -1;
	}

	private static int findEndingOffset(String text, int offset) {
		String substring = text.substring(offset);
		String[] split = substring.split("[^a-zA-Z0-9]+");//$NON-NLS-1$
		if(split.length == 0) {
			return -1;
		}
		return offset + split[0].length();
	}

	public void install(ITextViewer viewer) {
		if (!(viewer instanceof ISourceViewer)) {
			return;
		}
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(GenericEditorPlugin.BUNDLE_ID);
		preferences.addPreferenceChangeListener(this);
		this.enabled = preferences.getBoolean(ToggleHighlight.TOGGLE_HIGHLIGHT_PREFERENCE, true);
		this.sourceViewer = (ISourceViewer) viewer;
		this.sourceViewer.getTextWidget().addCaretListener(this);
	}

	public void uninstall() {
		if (sourceViewer != null) {
			sourceViewer.getTextWidget().removeCaretListener(this);
		}
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(GenericEditorPlugin.BUNDLE_ID);
		preferences.removePreferenceChangeListener(this);
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (event.getKey().equals(ToggleHighlight.TOGGLE_HIGHLIGHT_PREFERENCE)) {
			this.enabled = Boolean.parseBoolean(event.getNewValue().toString());
			if (enabled) {
				initialReconcile();
			} else {
				removeOccurrenceAnnotations();
			}
		}
	}

	@Override
	public void caretMoved(CaretEvent event) {
		applyHighlights(event.caretOffset);
	}

	@Override
	public void initialReconcile() {
		if (sourceViewer != null) {
			sourceViewer.getTextWidget().getDisplay().asyncExec(() -> {
				if (sourceViewer != null && sourceViewer.getTextWidget() != null) {
					applyHighlights(sourceViewer.getTextWidget().getCaretOffset());
				}
			});
		}
	}

	void removeOccurrenceAnnotations() {
		IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
		if (annotationModel == null || fOccurrenceAnnotations == null) {
			return;
		}

		synchronized (getLockObject(annotationModel)) {
			if (annotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension) annotationModel).replaceAnnotations(fOccurrenceAnnotations, null);
			} else {
				for (Annotation fOccurrenceAnnotation : fOccurrenceAnnotations) {
					annotationModel.removeAnnotation(fOccurrenceAnnotation);
				}
			}
			fOccurrenceAnnotations = null;
		}
	}

	private static Object getLockObject(IAnnotationModel annotationModel) {
		if (annotationModel instanceof ISynchronizable) {
			Object lock = ((ISynchronizable) annotationModel).getLockObject();
			if (lock != null) {
				return lock;
			}
		}
		return annotationModel;
	}

	@Override
	public void setDocument(IDocument document) {
		this.document = document;
	}

	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		// Do nothing
	}

	@Override
	public void reconcile(IRegion partition) {
		// Do nothing
	}

	@Override
	public void setProgressMonitor(IProgressMonitor monitor) {
		// Not used
	}
}

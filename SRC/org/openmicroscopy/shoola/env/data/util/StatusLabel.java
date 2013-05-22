/*
 * org.openmicroscopy.shoola.agents.treeviewer.util.StatusLabel
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2013 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.env.data.util;


//Java imports
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import ome.formats.importer.IObservable;
import ome.formats.importer.IObserver;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportEvent;
import ome.formats.importer.ImportEvent.FILESET_UPLOAD_END;
import ome.formats.importer.util.ErrorHandler;
import omero.cmd.CmdCallback;
import omero.cmd.CmdCallbackI;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.openmicroscopy.shoola.env.data.ImportException;
import org.openmicroscopy.shoola.util.ui.UIUtilities;

import pojos.DataObject;
import pojos.FilesetData;
import pojos.PixelsData;

/**
 * Component displaying the status of a specific import.
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @author Blazej Pindelski, bpindelski at dundee.ac.uk
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class StatusLabel
	extends JPanel
	implements IObserver
{
	
	/** The text displayed when the file is already selected.*/
	public static final String DUPLICATE = "Already processed, skipping";
	
	/** The text displayed when loading the image to import. */
	public static final String PREPPING_TEXT = "prepping";
	
	/** The text indicating the scanning steps. */
	public static final String SCANNING_TEXT = "Scanning...";
	
	/** 
	 * Bound property indicating that the original container has been reset.
	 * */
	public static final String NO_CONTAINER_PROPERTY = "noContainer";
	
	/** Bound property indicating that children files have been set. */
	public static final String FILES_SET_PROPERTY = "filesSet";
	
	/** 
	 * Bound property indicating that the file has to be reset
	 * This should be invoked if the log file for example has been selected. 
	 */
	public static final String FILE_RESET_PROPERTY = "fileReset";
	
	/** Bound property indicating that the import of the file has started. */
	public static final String FILE_IMPORT_STARTED_PROPERTY =
		"fileImportStarted";

	/** 
	 * Bound property indicating that the container corresponding to the
	 * folder has been created. 
	 * */
	public static final String CONTAINER_FROM_FOLDER_PROPERTY =
		"containerFromFolder";
	
	/** Bound property indicating that the status has changed.*/
	public static final String CANCELLABLE_IMPORT_PROPERTY =
		"cancellableImport";
	
	/** Bound property indicating that the status has changed.*/
	public static final String CANCELLED_IMPORT_PROPERTY = "cancelledImport";
	
	/** Bound property indicating that the debug text has been sent.*/
	public static final String DEBUG_TEXT_PROPERTY = "debugText";
	
	/** Bound property indicating that the import is done. */
	public static final String IMPORT_DONE_PROPERTY = "importDone";
	
	/** Bound property indicating that the upload is done. */
	public static final String UPLOAD_DONE_PROPERTY = "uploadDone";
	
	/** Bound property indicating that the scanning has started. */
	public static final String SCANNING_PROPERTY = "scanning";
	
	/** The default text of the component.*/
	private static final String DEFAULT_TEXT = "Pending...";

	/** Text to indicate that the import is cancelled. */
	private static final String CANCEL_TEXT = "cancelled";

	/** Text to indicate that no files to import. */
	private static final String NO_FILES_TEXT = "No Files to Import.";

	/** The width of the upload bar.*/
	private static final int WIDTH = 200;
	
	/** The maximum number of value for upload.*/
	private static final int MAX = 100;
	
	/** 
	 * The number of processing sets.
	 * 1. Importing Metadata
	 * 2. Processing Pixels
	 * 3. Generating Thumbnails
	 * 4. Processing Metadata
	 * 5. Generating Objects
	 */
	/** Map hosting the description of each steps.*/
	private static final Map<Integer, String> STEPS;
	
	static {
		STEPS = new HashMap<Integer, String>();
		STEPS.put(1, "Importing Metadata");
		STEPS.put(2, "Processing Pixels");
		STEPS.put(3, "Generating Thumbnails");
		STEPS.put(4, "Processing Metadata");
		STEPS.put(5, "Generating Objects");
		STEPS.put(6, "Complete");
	}
	
	/** The number of images in a series. */
	private int seriesCount;
	
	/** The type of reader used. */
	private String readerType;
	
	/** The files associated to the file that failed to import. */
	private String[] usedFiles;
	
	/** Flag indicating that the import has been cancelled. */
	private boolean markedAsCancel;
	
	/** Flag indicating that the import can or not be cancelled.*/
	private boolean cancellable;
	
	/** 
	 * Flag indicating that the file has already been imported or already
	 * in the queue.
	 */
	private boolean markedAsDuplicate;
	
	/** The size of the file.*/
	private String fileSize;
	
	/** The size units.*/
	private String units;
	
	/** The total size of uploaded files.*/
	private long totalUploadedSize;
	
	/** The label displaying the general import information.*/
	private JLabel generalLabel;
	
	/** Indicate the progress of the upload.*/
	private JProgressBar uploadBar;
	
	/** Indicate the progress of the processing.*/
	private JProgressBar processingBar;

	/** The size of the upload,*/
	private long sizeUpload;
	
	/** The labels displaying information before the progress bars.*/
	private List<JLabel> labels;

	/** Checksum event stored for later retrieval */
	private FILESET_UPLOAD_END checksumEvent;
	
	/** The exception if an error occurred.*/
	private ImportException exception;
	
	/** The list of pixels' identifiers returned when the import is complete.*/
	private Set<PixelsData> pixels;
	
	/** The file associated to that import.*/
	private FilesetData fileset;
	
	/** Flag indicating if the image is a HCS file or not.*/
	private boolean hcs;
	
	/** The callback. This should only be set when importing a directory.*/
	private CmdCallback callback;
	
	/** 
	 * Formats the size of the uploaded data.
	 * 
	 * @param value The value to display.
	 * @return See above.
	 */
	private String formatUpload(long value)
	{
		StringBuffer buffer = new StringBuffer();
		String v = FileUtils.byteCountToDisplaySize(value);
		String[] values = v.split(" ");
		if (values.length > 1) {
			String u = values[1];
			if (units.equals(u)) buffer.append(values[0]);
			else buffer.append(v);
		} else buffer.append(v);
		buffer.append("/");
		buffer.append(fileSize);
		return buffer.toString();
	}

	/** Builds and lays out the UI.*/
	private void buildUI()
	{
		labels = new ArrayList<JLabel>();
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(generalLabel);
		JLabel label = new JLabel("Upload");
		label.setVisible(false);
		labels.add(label);
		add(label);
		add(uploadBar);
		add(Box.createHorizontalStrut(5));
		label = new JLabel("Processing");
		label.setVisible(false);
		labels.add(label);
		add(label);
		add(processingBar);
		setOpaque(false);
	}

	/** Initializes the components.*/
	private void initiliaze()
	{
		sizeUpload = 0;
		fileSize = "";
		seriesCount = 0;
		readerType = "";
		markedAsCancel = false;
		cancellable = true;
		totalUploadedSize = 0;
		generalLabel = new JLabel(DEFAULT_TEXT);
		Font f = generalLabel.getFont();
		Font derived = f.deriveFont(f.getStyle(), f.getSize()-2);
		uploadBar = new JProgressBar(0, MAX);
		uploadBar.setFont(derived);
		uploadBar.setStringPainted(true);
		Dimension d = uploadBar.getPreferredSize();
		uploadBar.setPreferredSize(new Dimension(WIDTH, d.height));
		processingBar = new JProgressBar(0, STEPS.size());
		processingBar.setStringPainted(true);
		processingBar.setString(DEFAULT_TEXT);
		processingBar.setFont(derived);
		uploadBar.setVisible(false);
		processingBar.setVisible(false);
	}

	/**
	 * Handles error that occurred during the processing.
	 * 
	 * @param text The text to display if any.
	 */
	private void handleProcessingError(String text)
	{
		generalLabel.setText(text);
		cancellable = false;
		firePropertyChange(CANCELLABLE_IMPORT_PROPERTY, null, this);
	}

	/** Creates a new instance. */
	public StatusLabel()
	{
		initiliaze();
		buildUI();
	}

	/** 
	 * Sets the file set when the upload is complete.
	 * To be modified.
	 * 
	 * @param fileset The value to set.
	 */
	public void setFilesetData(final FilesetData fileset)
	{
		this.fileset = fileset;
	}
	
	/**
	 * Sets to <code>true</code> if it is a HCS file, <code>false</code>
	 * otherwise.
	 * 
	 * @param hcs Pass <code>true</code> if it is a HCS file, <code>false</code>
	 * otherwise.
	 */
	public void setHCS(boolean hcs) { this.hcs = hcs; }
	
	/**
	 * Returns <code>true</code> if it is a HCS file, <code>false</code>
	 * otherwise.
	 * 
	 * @return See above.
	 */
	public boolean isHCS() { return hcs; }
	
	/**
	 * Returns the file set associated to the import.
	 * 
	 * @return See above.
	 */
	public FilesetData getFileset() { return fileset; }
	
	/**
	 * Sets the collection of files to import.
	 * 
	 * @param usedFiles The value to set.
	 */
	public void setUsedFiles(String[] usedFiles)
	{
		this.usedFiles = usedFiles;
		if (usedFiles == null) return;
		for (int i = 0; i < usedFiles.length; i++) {
			sizeUpload += (new File(usedFiles[i])).length();
		}
		fileSize = FileUtils.byteCountToDisplaySize(sizeUpload);
		String[] values = fileSize.split(" ");
		if (values.length > 1) units = values[1];
	}

	/**
	 * Sets the callback. This method should only be invoked when the 
	 * file is imported from a folder.
	 * 
	 * @param cmd The object to handle.
	 */
	public void setCallback(Object cmd)
	{
		if (cmd instanceof ImportException) exception = (ImportException) cmd;
		else if (cmd instanceof CmdCallback) callback = (CmdCallback) cmd;
		firePropertyChange(UPLOAD_DONE_PROPERTY, null, this);
	}

	/** Marks the import has cancelled. */
	public void markedAsCancel()
	{
		generalLabel.setText(CANCEL_TEXT);
		this.markedAsCancel = true;
	}
	
	/**
	 * Returns <code>true</code> if the import is marked as cancel,
	 * <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	public boolean isMarkedAsCancel() { return markedAsCancel; }

	/** Marks the import has duplicate. */
	public void markedAsDuplicate()
	{
		this.markedAsDuplicate = true;
		generalLabel.setText(DUPLICATE);
	}

	/**
	 * Returns <code>true</code> if the import is marked as duplicate,
	 * <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	public boolean isMarkedAsDuplicate() { return markedAsDuplicate; }

	/**
	 * Returns the text if an error occurred.
	 * 
	 * @return See above.
	 */
	public String getErrorText() { return ""; }

	/**
	 * Returns the type of reader used.
	 * 
	 * @return See above.
	 */
	public String getReaderType() { return readerType; }

	/**
	 * Returns the files associated to the file failing to import.
	 * 
	 * @return See above.
	 */
	public String[] getUsedFiles() { return usedFiles; }

	/**
	 * Returns the source files that have checksum values or <code>null</code>
	 * if no event stored.
	 * 
	 * @return See above.
	 */
	public List<String> getChecksums()
	{
		if (!hasChecksum()) return null;
		return checksumEvent.checksums;
	}
	
	/**
	 * Returns the checksum values or <code>null</code> if no event stored.
	 * 
	 * @return See above.
	 */
	public Map<Integer, String> getFailingChecksums()
	{
		if (!hasChecksum()) return null;
		return checksumEvent.failingChecksums;
	}
	
	/**
	 * Returns the source files that have checksum values or <code>null</code>
	 * if no event stored.
	 * 
	 * @return See above.
	 */
	public String[] getChecksumFiles()
	{
		if (!hasChecksum()) return null;
		return checksumEvent.srcFiles;
	}
	

	/** 
	 * Returns <code>true</code> if the checksums have been calculated,
	 * <code>false</code> otherwise.
	 * 
	 * @return
	 */
	public boolean hasChecksum() { return checksumEvent != null; }

	/**
	 * Fires a property indicating to import the files.
	 * 
	 * @param files The file to handle.
	 */
	public void setFiles(Map<File, StatusLabel> files)
	{
		generalLabel.setText(NO_FILES_TEXT);
		if (!CollectionUtils.isEmpty(files.entrySet())) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Importing ");
			buffer.append(files.size());
			buffer.append(" file");
			if (files.size() > 1) buffer.append("s");
			generalLabel.setText(buffer.toString());
		}
		firePropertyChange(FILES_SET_PROPERTY, null, files);
	}

	/**
	 * Indicates that the original container has been reset.
	 */
	public void setNoContainer()
	{
		firePropertyChange(NO_CONTAINER_PROPERTY,
				Boolean.valueOf(false), Boolean.valueOf(true));
	}

	/**
	 * Sets the container corresponding to the folder.
	 * 
	 * @param container The container to set.
	 */
	public void setContainerFromFolder(DataObject container)
	{
		firePropertyChange(CONTAINER_FROM_FOLDER_PROPERTY, null, container);
	}

	/**
	 * Replaces the initial file by the specified one. This should only be
	 * invoked if the original file was an arbitrary one requiring to use the
	 * import candidate e.g. <code>.log</code>.
	 * 
	 * @param file The new file.
	 */
	public void resetFile(File file)
	{
		firePropertyChange(FILE_RESET_PROPERTY, null, file);
	}

	/**
	 * Returns the number of series.
	 * 
	 * @return See above.
	 */
	public int getSeriesCount() { return seriesCount; }

	/**
	 * Returns <code>true</code> if the import can be cancelled,
	 * <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	public boolean isCancellable() { return cancellable; }

	/**
	 * Returns the result of the import either a collection of
	 * <code>PixelsData</code> or an exception.
	 * 
	 * @return See above.
	 */
	public Object getImportResult()
	{
		if (exception != null) return exception;
		if (pixels != null) return pixels;
		return callback;
	}

	/**
	 * Returns the size of the upload.
	 * 
	 * @return See above.
	 */
	public long getFileSize() { return sizeUpload; }

	/**
	 * Displays the status of an on-going import.
	 * @see IObserver#update(IObservable, ImportEvent)
	 */
	public void update(IObservable observable, ImportEvent event)
	{
		if (event == null) return;
		cancellable = false;
		if (event instanceof ImportEvent.IMPORT_DONE) {
			pixels = (Set<PixelsData>) PojoMapper.asDataObjects(
					((ImportEvent.IMPORT_DONE) event).pixels);
			firePropertyChange(IMPORT_DONE_PROPERTY, null, this);
		} else if (event instanceof ImportCandidates.SCANNING) {
			if (!markedAsCancel && exception == null)
				generalLabel.setText(SCANNING_TEXT);
			firePropertyChange(SCANNING_PROPERTY, null, this);
		} else if (event instanceof ErrorHandler.FILE_EXCEPTION) {
			ErrorHandler.FILE_EXCEPTION e = (ErrorHandler.FILE_EXCEPTION) event;
			readerType = e.reader;
			usedFiles = e.usedFiles;
			exception = new ImportException(e.exception);
			handleProcessingError("");
		} else if (event instanceof ErrorHandler.UNKNOWN_FORMAT) {
			exception = new ImportException(ImportException.UNKNOWN_FORMAT_TEXT,
					((ErrorHandler.UNKNOWN_FORMAT) event).exception);
			handleProcessingError(ImportException.UNKNOWN_FORMAT_TEXT);
		} else if (event instanceof ErrorHandler.MISSING_LIBRARY) {
			exception = new ImportException(ImportException.MISSING_LIBRARY_TEXT,
					((ErrorHandler.MISSING_LIBRARY) event).exception);
			handleProcessingError(ImportException.MISSING_LIBRARY_TEXT);
		} else if (event instanceof ImportEvent.FILE_UPLOAD_BYTES) {
			ImportEvent.FILE_UPLOAD_BYTES e =
				(ImportEvent.FILE_UPLOAD_BYTES) event;
			long v = totalUploadedSize+e.uploadedBytes;
			uploadBar.setValue((int) (v*MAX/sizeUpload));
			StringBuffer buffer = new StringBuffer();
			if (v != sizeUpload) buffer.append(formatUpload(v));
			else  buffer.append(fileSize);
			buffer.append(" ");
			if (e.timeLeft != 0) {
				String s = UIUtilities.calculateHMSFromMilliseconds(e.timeLeft,
						true);
				buffer.append(s);
				if (!StringUtils.isBlank(s)) buffer.append(" Left");
				else buffer.append("Almost complete");
			}
			uploadBar.setString(buffer.toString());
		} else if (event instanceof ImportEvent.FILE_UPLOAD_COMPLETE) {
			ImportEvent.FILE_UPLOAD_COMPLETE e =
				(ImportEvent.FILE_UPLOAD_COMPLETE) event;
			totalUploadedSize += e.uploadedBytes;
		} else if (event instanceof ImportEvent.FILESET_UPLOAD_END) {
            checksumEvent = (ImportEvent.FILESET_UPLOAD_END) event;
            if (exception == null) {
    			processingBar.setValue(1);
    			processingBar.setString(STEPS.get(1));
            }
		} else if (event instanceof ImportEvent.METADATA_IMPORTED) {
			processingBar.setValue(2);
			processingBar.setString(STEPS.get(2));
		} else if (event instanceof ImportEvent.PIXELDATA_PROCESSED) {
			processingBar.setValue(3);
			processingBar.setString(STEPS.get(3));
		} else if (event instanceof ImportEvent.THUMBNAILS_GENERATED) {
			processingBar.setValue(4);
			processingBar.setString(STEPS.get(4));
		} else if (event instanceof ImportEvent.METADATA_PROCESSED) {
			processingBar.setValue(5);
			processingBar.setString(STEPS.get(5));
		} else if (event instanceof ImportEvent.OBJECTS_RETURNED) {
			processingBar.setValue(6);
			processingBar.setString(STEPS.get(6));
		} else if (event instanceof ImportEvent.FILESET_UPLOAD_START) {
			Iterator<JLabel> i = labels.iterator();
			while (i.hasNext()) {
				i.next().setVisible(true);
			}
			generalLabel.setText("");
			uploadBar.setVisible(true);
			processingBar.setVisible(true);
			firePropertyChange(FILE_IMPORT_STARTED_PROPERTY, null, this);
		} else if (event instanceof ErrorHandler.INTERNAL_EXCEPTION) {
			ErrorHandler.INTERNAL_EXCEPTION e =
					(ErrorHandler.INTERNAL_EXCEPTION) event;
			readerType = e.reader;
			usedFiles = e.usedFiles;
			exception = new ImportException(e.exception);
			handleProcessingError("");
		}
	}

}

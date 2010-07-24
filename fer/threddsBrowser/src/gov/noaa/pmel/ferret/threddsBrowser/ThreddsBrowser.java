/**
 *  This software was developed by the Thermal Modeling and Analysis
 *  Project(TMAP) of the National Oceanographic and Atmospheric
 *  Administration's (NOAA) Pacific Marine Environmental Lab(PMEL),
 *  hereafter referred to as NOAA/PMEL/TMAP.
 *
 *  Access and use of this software shall impose the following
 *  obligations and understandings on the user. The user is granted the
 *  right, without any fee or cost, to use, copy, modify, alter, enhance
 *  and distribute this software, and any derivative works thereof, and
 *  its supporting documentation for any purpose whatsoever, provided
 *  that this entire notice appears in all copies of the software,
 *  derivative works and supporting documentation.  Further, the user
 *  agrees to credit NOAA/PMEL/TMAP in any publications that result from
 *  the use of this software or in any product that includes this
 *  software. The names TMAP, NOAA and/or PMEL, however, may not be used
 *  in any advertising or publicity to endorse or promote any products
 *  or commercial entity unless specific written permission is obtained
 *  from NOAA/PMEL/TMAP. The user also understands that NOAA/PMEL/TMAP
 *  is not obligated to provide the user with any support, consulting,
 *  training or assistance of any kind with regard to the use, operation
 *  and performance of this software nor to provide the user with any
 *  updates, revisions, new versions or "bug fixes".
 *
 *  THIS SOFTWARE IS PROVIDED BY NOAA/PMEL/TMAP "AS IS" AND ANY EXPRESS
 *  OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL NOAA/PMEL/TMAP BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
 *  RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
 *  CONTRACT, NEGLIGENCE OR OTHER TORTUOUS ACTION, ARISING OUT OF OR IN
 *  CONNECTION WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE. 
 */
package gov.noaa.pmel.ferret.threddsBrowser;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.ui.CatalogTreeView;
import ucar.util.prefs.PreferencesExt;

/**
 * Creates a THREDDS catalog browser for simple selection of a dataset.
 * Uses a UCAR/Unidata CatalogTreeView to do much of the work.
 * @author Karl M. Smith - karl.smith (at) noaa.gov
 */
public class ThreddsBrowser extends JPanel {

	private static final long serialVersionUID = 1969846472125911914L;

	/** Default locations environment variable - used for reset */
	private String defLocsEnvName;

	/**	Original BrowserDefaults - used for reset */
	private BrowserDefaults initialDefaults;

	/** JComboBox of the current and previous locations */
	private JComboBox locationsCombo;

	/** JLabel showing the currently displayed location */
	private JLabel locationLabel;

	/** Tree view of the catalog or file system */
	private CatalogTreeView treeViewer;

	/** Display of information on the selected dataset */
	private HTMLViewer htmlViewer;

	/** JSplitPane containing treeViewer and htmlViewer */
	private JSplitPane splitPanel;

	/** The "USE" button */
	private JButton useButton;

	/** The "Cancel" button */
	private JButton cancelButton;

	/** Current selected dataset URL */
	private String datasetName;

	/** JLabel displaying the currently selected dataset */
	private JLabel datasetLabel;

	/** Default directory for the local directory file chooser */ 
	private File localBrowseDir;

	/** A comma/semicolon/space separated list of acceptable filename extensions for local datasets */
	private String extensionsString;

	/**
	 * Create a THREDDS catalog browser for simple selection of a dataset.
	 * @param prefs the default preference settings for this ThreddsBrowers; 
	 * @param defaultLocationsEnvName the name of the locations environment variable 
	 * whose value is a space-separated list of possibly-quoted locations.  May be 
	 * null.  These locations, if not also given in prefs, will appear in 
	 * order at the bottom of the drop-down list.  
	 */
	public ThreddsBrowser(PreferencesExt prefs, String defaultLocationsEnvName) {
		// Get the defaults
		defLocsEnvName = defaultLocationsEnvName;
		initialDefaults = new BrowserDefaults(prefs, defLocsEnvName);

		// Use a grid bag layout in this JPanel
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;

		// JLabel in front of the drop-down list of locations
		JLabel newLocationLabel = new JLabel("New location:");
		gbc.gridx = 0;     gbc.gridy = 0;
		gbc.gridwidth = 1; gbc.gridheight = 1;
	    gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(6,2,6,2);
		add(newLocationLabel, gbc);

		// Editable drop-down list of locations
		locationsCombo = new JComboBox();
		locationsCombo.setEditable(true);
		gbc.gridx = 1;     gbc.gridy = 0;
		gbc.gridwidth = 1; gbc.gridheight = 1;
	    gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2,2,2,2);
		add(locationsCombo, gbc);

		// Open button to open the current selected location
		JButton showButton = new JButton("Show");
		showButton.setFocusPainted(false);
		showButton.setToolTipText("Display the new location below");
		gbc.gridx = 2;     gbc.gridy = 0;
		gbc.gridwidth = 1; gbc.gridheight = 1;
	    gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2,2,2,2);
		add(showButton, gbc);

		// Browse button for selecting a local directory
		JButton localBrowseButton = new JButton("Local");
		localBrowseButton.setFocusPainted(false);
		localBrowseButton.setToolTipText("Find a local directory to display");
		gbc.gridx = 3;     gbc.gridy = 0;
		gbc.gridwidth = 1; gbc.gridheight = 1;
	    gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2,2,2,2);
		add(localBrowseButton, gbc);

		// Separator
		JSeparator separator = new JSeparator();
		gbc.gridx = 0;     gbc.gridy = 1;
		gbc.gridwidth = 4; gbc.gridheight = 1;
	    gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2,2,2,2);
		add(separator, gbc);

		// JLabel showing the currently displayed location
		locationLabel = new JLabel("Shown location: ");
		gbc.gridx = 0;     gbc.gridy = 2;
		gbc.gridwidth = 4; gbc.gridheight = 1;
	    gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2,2,2,2);
		add(locationLabel, gbc);

		// CatalogTreeView for viewing the catalog or directory tree
		treeViewer = new CatalogTreeView();

		// HTMLViewer displaying information on the selected dataset
		htmlViewer = new HTMLViewer();

	    // JSplitPane containing the above treeViewer on the left and htmlViewerScrollPane on the right
	    splitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, treeViewer, htmlViewer);
		gbc.gridx = 0;     gbc.gridy = 3;
		gbc.gridwidth = 4; gbc.gridheight = 1;
	    gbc.weightx = 1.0; gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2,2,2,2);
		add(splitPanel, gbc);

		// USE button to output the current dataset and close 
		useButton = new JButton("USE");
		useButton.setFocusPainted(false);
		useButton.setToolTipText("Use the selected dataset and exit this browser");
		gbc.gridx = 0;     gbc.gridy = 4;
		gbc.gridwidth = 1; gbc.gridheight = 1;
	    gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2,2,2,2);
		add(useButton, gbc);

		// JLabel with the current selected dataset name
		datasetName = " ";
		datasetLabel = new JLabel(datasetName);
		datasetLabel.setToolTipText("The currently selected dataset");
		gbc.gridx = 1;     gbc.gridy = 4;
		gbc.gridwidth = 1; gbc.gridheight = 1;
	    gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(6,2,6,2);
		add(datasetLabel, gbc);

		// Reset button to reset the default settings
		JButton resetButton = new JButton("Reset");
		resetButton.setFocusPainted(false);
		resetButton.setToolTipText("Reset this browser with default settings");
		gbc.gridx = 2;     gbc.gridy = 4;
		gbc.gridwidth = 1; gbc.gridheight = 1;
	    gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2,2,2,2);
		add(resetButton, gbc);
		
		// Cancel button to close without output of a dataset name
		cancelButton = new JButton("Cancel");
		cancelButton.setFocusPainted(false);
		cancelButton.setToolTipText("Exit this browser without designating a dataset to use");
		gbc.gridx = 3;     gbc.gridy = 4;
		gbc.gridwidth = 1; gbc.gridheight = 1;
	    gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2,2,2,2);
		add(cancelButton, gbc);

		// Set the defaults
		resetDefaults(initialDefaults);

		// Listen for Open button presses
		showButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showSelectedLocation();
			}
		});

		// Listen for Browse button presses
		localBrowseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browseLocalDirectories();
			}
		});

		// Listen for single- and double clicks in the CatalogTreeView
		treeViewer.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				String propName = e.getPropertyName();
				if ( "Selection".equals(propName) ) {
					// Single-click (or first click of a double-click) on a dataset or file name
					selectDataset();
				}
				else if ( "Dataset".equals(propName) || "File".equals(propName) ) {
					// Second click of a double-click on a dataset or file name
					useButton.doClick();
				}
			}
		});

		// Listen for hyperlink selections in the htmlViewer
		htmlViewer.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if ( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
					// Don't bother dealing with frames
					try {
						htmlViewer.setPage(e.getURL());
					} catch (Exception exc) {
						;
					}
				}
			}
		});

		// Listen for Reset button presses
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Ask which settings to return to
				String[] resetOptions = {"start-up settings", "default settings"};
				String result = (String) JOptionPane.showInputDialog(ThreddsBrowser.this, "Reset to: ",
												"Reset Settings", JOptionPane.INFORMATION_MESSAGE, null, 
												resetOptions, resetOptions[0]);
				if ( resetOptions[0].equals(result) ) {
					// start-up settings - use the defaults created in the constructor
					resetDefaults(initialDefaults);
				}
				else if ( resetOptions[1].equals(result) ) {
					// default settings - use defaults from not using a XMLStore/PreferencesExt
					resetDefaults(new BrowserDefaults(null, defLocsEnvName));
				}
			}
		});
	}

	/**
	 * Set the displayed location to the one selected in the locationsCombo.
	 */
	private void showSelectedLocation() {
		String location = (String) locationsCombo.getSelectedItem();
		showLocation(location);
	}

	/**
	 * Display the location to the given URI string, URL string, 
	 * or local directory pathname.
	 * @param location the location to open; if null or blank, does nothing.
	 */
	public void showLocation(String location) {
		if ( location == null ) 
			return;
		String trimmedLocation = location.trim();
		if ( trimmedLocation.isEmpty() )
			return;

		URI uri;
		try {
			// Create a URI from the given location string
			uri = new URI(trimmedLocation);
		} catch (URISyntaxException e) {
			// If problems, assume this is a local file pathname
			File locFile = new File(trimmedLocation);
			uri = locFile.toURI();
		}

		String scheme = uri.getScheme();
		if ( (scheme == null) || "file".equals(scheme) ) {
			showLocalLocation(new File(uri.getPath()));
		}
		else {
			showThreddsServerLocation(trimmedLocation);
		}
	}

	/**
	 * Display the THREDDS server catalog at the given URI.
	 * @param locationURI the URI of the THREDDS server catalog
	 */
	private void showThreddsServerLocation(String locationString) {
		// Update the location displayed in the browser and clear the viewers
		clearViewers();
		updateLocationLabels(locationString);

		// Display the location in the tree viewer
		treeViewer.setCatalog(locationString);
	}

	/**
	 * Create and display a catalog of the given local directory.
	 * Before creating the catalog, a dialog is opened requesting 
	 * the user for filename extensions of datasets to display.
	 * @param localDir the local directory to display
	 */
	private void showLocalLocation(File localDir) {
		// Prompt the user for the file filters
		String extsString = JOptionPane.showInputDialog(this, "Show datasets with the filename extension\n" +
														"(give none to show all files):", extensionsString);
		// If the user cancelled out of the filters dialog, just return
		if ( extsString == null )
			return;

		// Create the Collection of individual extension Strings
		Collection<String> extsColl = BrowserDefaults.parseExtensionsString(extsString);

		// Create a new standard String of extensions
		extensionsString = BrowserDefaults.createExtensionsString(extsColl);

		// Create the catalog for this directory
		LocalDirTreeScanMonitor scanMonitor;
		try {
			scanMonitor = new LocalDirTreeScanMonitor(this, localDir, new ExtensionFileFilter(extsColl));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Unable to catalog " + localDir + "\n" + e.getMessage(), 
										  "Unable to Catalog", JOptionPane.ERROR_MESSAGE);
			return;
		}
		scanMonitor.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String propName = evt.getPropertyName();
				if ( "Done".equals(propName) ) {
					// Success - get the local directory tree root and the generated catalog
					File rootDir = (File) evt.getOldValue();
					InvCatalogImpl catalog = (InvCatalogImpl) evt.getNewValue();

					// Update the location displayed in the browser and clear the viewers
					clearViewers();
					updateLocationLabels(rootDir.getPath());

					// Display the catalog in the tree viewer
					treeViewer.setCatalog(catalog);
				}
				else if ( "Canceled".equals(propName) ) {
					// Scan was canceled by the user
					JOptionPane.showMessageDialog(ThreddsBrowser.this, "Scan canceled", 
												  "Scan Canceled", JOptionPane.ERROR_MESSAGE);
				}
				else if ( "Died".equals(propName) ) {
					// Scan threw an exception
					Throwable cause = (Throwable) evt.getNewValue();
					JOptionPane.showMessageDialog(ThreddsBrowser.this, "Scan died: " + cause.getMessage(), 
												  "Scan Died", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		scanMonitor.runScan();
	}
	
	/**
	 * Clears the shown location label, treeViewer and htmlViewer
	 */
	private void clearViewers() {
		treeViewer.getJTree().setModel(new DefaultTreeModel(new DefaultMutableTreeNode(null, false)));
		htmlViewer.clearPage();
		locationLabel.setText("Shown location: ");
	}

	/**
	 * Updates the drop-down list of locations as well as the label of currently
	 * shown location.  Clears the tree viewer and the HTML info viewer.
	 * @param location the new location (trimmed; not null)
	 */
	private void updateLocationLabels(String location) {
		// Remove the location from the list, so there are no duplicates
		locationsCombo.removeItem(location);

		// Add the (possibly new) location to the list
		locationsCombo.insertItemAt(location, 0);

		// Make sure the location is displayed
		locationsCombo.setSelectedIndex(0);

		// Update the currently show location label
		locationLabel.setText("Shown location: " + location);
	}

	/**
	 * Select a directory from the local file system.
	 */
	private void browseLocalDirectories() {
		// Create a file chooser opened to the default local directory
		JFileChooser chooser = new JFileChooser(localBrowseDir);

		// Customize the file chooser to suit our needs
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select Directory of Datasets to Show");
		chooser.setApproveButtonToolTipText("Show the datasets in the selected directory");

		// Get the selected directory
		if ( chooser.showDialog(this, "Show") == JFileChooser.APPROVE_OPTION ) {
			localBrowseDir = chooser.getSelectedFile();
			showLocalLocation(localBrowseDir);
		}
	}

	/**
	 * Set the currently selected dataset name and show information about it.
	 */
	private void selectDataset() {
		// Get the dataset name
		InvDatasetImpl dataset = (InvDatasetImpl) treeViewer.getSelectedDataset();

		// Assign datasetName if the selected item is actually a dataset (has an access)
		datasetName = " ";
		if ( dataset != null ) {
			List<InvAccess> accesses = dataset.getAccess();
			if ( accesses.size() > 0 ) {
				URI uri = accesses.get(0).getStandardUri();
				String scheme = uri.getScheme();
				if ( (scheme == null) || "file".equals(scheme) ) {
					// for local datasets, get the path from the dataset ID
					datasetName = dataset.getID();
				}
				else {
					// for remote datasets, show the URI
					datasetName = accesses.get(0).getStandardUrlName();
				}
			}
		}

		// Clear the info viewer so there is no mismatch while new info is being created
		htmlViewer.clearPage();

		// Show the new dataset name
		datasetLabel.setText(datasetName);

		// If actually a dataset, create content in the info viewer
		if ( ! datasetName.trim().isEmpty() ) {
			StringBuilder sbuff = new StringBuilder(2048);
			InvDatasetImpl.writeHtmlDescription(sbuff, dataset, false, true, false, false, true);
			htmlViewer.showHTMLBodyText(sbuff.toString());
		}
	}

	/**
	 * @return the name of the currently selected dataset, 
	 * or null is there is no currently selected dataset. 
	 */
	public String getDatasetName() {
		if ( datasetName.trim().isEmpty() )
			return null;
		return datasetName;
	}

	/**
	 * Adds the given ActionListener to the "USE" button in the browser
	 */
	public void addUseActionListener(ActionListener actionListener) {
		useButton.addActionListener(actionListener);
	}

	/**
	 * Removes the given ActionListener from the "USE" button in the browser
	 */
	public void removeUseActionListener(ActionListener actionListener) {
		useButton.removeActionListener(actionListener);
	}

	/**
	 * Adds the given ActionListener to the "Cancel" button in the browser
	 */
	public void addCancelActionListener(ActionListener actionListener) {
		cancelButton.addActionListener(actionListener);
	}

	/**
	 * Removes the given ActionListener from the "Cancel" button in the browser
	 */
	public void removeCancelActionListener(ActionListener actionListener) {
		cancelButton.removeActionListener(actionListener);
	}

	/**
	 * Reset the default values in the browser to those given in the BrowserDefaults.
	 * @param defs the BrowserDefaults to use; cannot be null
	 */
	private void resetDefaults(BrowserDefaults defs) {
		// Set the initial items in the drop-down list of locations
		locationsCombo.removeAllItems();
		for (String loc: defs.getLocationStrings()) {
			locationsCombo.addItem(loc);
		}

		// Set the split pane divider location
	    splitPanel.setDividerLocation(defs.getDividerLocation());
		
		// Get the directory for the local directory file chooser 
		localBrowseDir = defs.getLocalBrowseDir();

		// Get the string of acceptable filename extensions for displayed datasets
		extensionsString = defs.getExtensionsString();

		// Clear the contents of the viewers
		clearViewers();

		// Clear the dataset name
		datasetName = " ";
		datasetLabel.setText(datasetName);

		// Set the preferred size of the browser panel
	    setPreferredSize(defs.getBrowserSize());
	}

	/**
	 * Save all the current settings of this ThreddsBrowser to prefs.
	 */
	public void savePreferences(PreferencesExt prefs) {
		// Save the list of locations
		int numItems = locationsCombo.getItemCount();
		ArrayList<String> locationsList = new ArrayList<String>(numItems);
		for (int k = 0; k < numItems; k++) {
			locationsList.add( (String) locationsCombo.getItemAt(k) );
		}
		BrowserDefaults.saveLocationsList(prefs, locationsList);

		// Save the divider location
		BrowserDefaults.saveDividerLocation(prefs, splitPanel.getDividerLocation());

		// Save the default directory for the local directory file chooser
		BrowserDefaults.saveLocalBrowseDir(prefs, localBrowseDir);

		// Save the standard String of extensions
		BrowserDefaults.saveExtensionsString(prefs, extensionsString);

		// Save the size of the browser panel
		BrowserDefaults.saveBrowserSize(prefs, getSize());
	}

	/**
	 * Creates the default filename for a ThreddsBrowser store file.
	 * Creates any missing directories in this filename.  
	 * @return the default filename, or null if unable to create
	 * missing directories
	 */
	public static String createDefaultStoreFilename() {
		String storeFilename = null;

		try {
			// First try Window desired location for application data stores
			// APPDATA not likely to be defined on Unix systems
			storeFilename = System.getenv("APPDATA");
		    if ( storeFilename != null ) {
		    	storeFilename += File.separator + "TMAP";
		    }
	    } catch (Exception e) {
	    	;
	    }

	    if ( storeFilename == null ) {
	    	// Now try in the user.home directory
	    	try {
	    		storeFilename = System.getProperty("user.home");
		    	if ( storeFilename != null ) {
		    		storeFilename += File.separator + ".tmap";
		    	}
	    	} catch (Exception e) {
	    		;
	    	}
	    }

	    if ( storeFilename == null ) {
	    	// If all else fails, put it under the current working directory
	    	storeFilename = ".tmap";
	    }

	    try {
		    // Create any missing directory path for the store file
	    	File storeDir = new File(storeFilename);
	    	if ( ! storeDir.exists() ) {
	    		if ( ! storeDir.mkdirs() ) {
	    			throw new IOException();
	    		}
	    	}

	    	// Add the store filename to the path
	    	storeFilename += File.separator + "ThreddsBrowser.xml";
	    } catch (Exception e) {
			storeFilename = null;
	    }

	    return storeFilename;
	}

	/**
	 * Starts the THREDDS catalog browser.  The first argument,
	 * if given, is used as the initial catalog URL in the browser.
	 */
	public static void main(final String[] args) {
		// Initialize the HTTPClient logger so that it won't complain
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "fatal");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "fatal");

		// Create the GUI in a separate thread managed by Swing
        SwingUtilities.invokeLater(new Runnable() {
        	@Override
            public void run() {
                ThreddsBrowserListener.createAndShowBrowser(args);
            }
        });
	}

}

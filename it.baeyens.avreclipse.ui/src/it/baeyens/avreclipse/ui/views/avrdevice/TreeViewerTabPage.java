/*******************************************************************************
 * Copyright (c) 2010 Thomas Holland (thomas@innot.de) and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: TreeViewerTabPage.java 851 2010-08-07 19:37:00Z innot $
 *******************************************************************************/
package it.baeyens.avreclipse.ui.views.avrdevice;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.CTabItem;

public class TreeViewerTabPage {

	private TreeViewer fTreeViewer = null;
	private CTabItem fTabItem = null;
	
	public void setTreeViewer(TreeViewer treeviewer) {
		fTreeViewer = treeviewer;
	}
	
	public TreeViewer getTreeViewer() {
		return fTreeViewer;
	}
	
	public void setCTabItem(CTabItem tabitem) {
		fTabItem = tabitem;
	}
	
	public CTabItem getCTabItem() {
		return fTabItem;
	}
}

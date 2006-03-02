/*
 * InfoGear.java
 * Copyright 2001 (C) Mario Bonassin
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * @author  Mario Bonassin
 * Created on April 21, 2001, 2:15 PM
 * Modified June 5, 2001 by Bryan McRoberts (merton_monk@yahoo.com)
 *
 * Current Ver: $Revision: 1.99 $
 * Last Editor: $Author: karianna $
 * Last Edited: $Date: 2006/02/15 16:32:42 $
 *
 */
package pcgen.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import pcgen.core.Constants;
import pcgen.core.Equipment;
import pcgen.core.EquipmentList;
import pcgen.core.GameMode;
import pcgen.core.Globals;
import pcgen.core.PObject;
import pcgen.core.PlayerCharacter;
import pcgen.core.SettingsHandler;
import pcgen.core.character.EquipSet;
import pcgen.core.character.WieldCategory;
import pcgen.core.prereq.PrereqHandler;
import pcgen.core.utils.CoreUtility;
import pcgen.core.utils.MessageType;
import pcgen.core.utils.ShowMessageDelegate;
import pcgen.gui.CharacterInfoTab;
import pcgen.gui.EQFrame;
import pcgen.gui.GuiConstants;
import pcgen.gui.PCGen_Frame1;
import pcgen.gui.filter.FilterAdapterPanel;
import pcgen.gui.filter.FilterConstants;
import pcgen.gui.filter.FilterFactory;
import pcgen.gui.panes.FlippingSplitPane;
import pcgen.gui.utils.AbstractTreeTableModel;
import pcgen.gui.utils.ClickHandler;
import pcgen.gui.utils.IconUtilitities;
import pcgen.gui.utils.JComboBoxEx;
import pcgen.gui.utils.JLabelPane;
import pcgen.gui.utils.JTreeTable;
import pcgen.gui.utils.JTreeTableMouseAdapter;
import pcgen.gui.utils.JTreeTableSorter;
import pcgen.gui.utils.LabelTreeCellRenderer;
import pcgen.gui.utils.PObjectNode;
import pcgen.gui.utils.ResizeColumnListener;
import pcgen.gui.utils.TreeTableModel;
import pcgen.gui.utils.Utility;
import pcgen.util.BigDecimalHelper;
import pcgen.util.InputFactory;
import pcgen.util.InputInterface;
import pcgen.util.Logging;
import pcgen.util.PropertyFactory;

/**
 *
 * This class is responsible for drawing the equipment related window
 * including indicating what items are available, which ones are selected
 * and handling the selection/de-selection of both.
 *
 * @author  Mario Bonassin
 * @version $Revision: 1.99 $
 **/
public final class InfoGear extends FilterAdapterPanel implements CharacterInfoTab
{
	static final long serialVersionUID = -2320970658737297916L;
	private static boolean needsUpdate = true;
	private static int splitOrientation = JSplitPane.HORIZONTAL_SPLIT;
	private static int viewMode = GuiConstants.INFOINVENTORY_VIEW_TYPE_NAME; // keep track of what view mode we're in for Available
	private static int viewSelectMode = GuiConstants.INFOINVENTORY_VIEW_NAME; // keep track of what view mode we're in for Selected. defaults to "Name"
	private static Integer saveAvailableViewMode = null;
	private static Integer saveSelectedViewMode = null;
	private static final int NUM_COL_AVAILABLE = 3;
	private static final int NUM_COL_SELECTED = 4;
	private static final int COL_NAME = 0;
	private static final int COL_COST = 1;
	private static final int COL_QTY_SRC = 2;
	private static final int COL_INDEX = 3;

	/**
	 * typeSubtypeRoot is the base structure used by both the available
	 * and selected tables; no need to generate this same list twice.
	 **/
	private static Object typeSubtypeRoot;
	private static Object typeRoot;
	private static Object allTypeRoot;
	private static Object sourceRoot;

	// Action listeners for standard buy/sell actions involving a single item
	private BuyGearActionListener buyOneListener = new BuyGearActionListener(1);
	private EQFrame eqFrame = null;
	private EquipmentModel availableModel = null; // Model for the JTreeTable.
	private EquipmentModel selectedModel = null; // Model for the JTreeTable.
	private FlippingSplitPane asplit;
	private FlippingSplitPane bsplit;
	private FlippingSplitPane splitPane;
	private JComboBoxEx cmbBuyPercent = new JComboBoxEx();
	private JComboBoxEx cmbSellPercent = new JComboBoxEx();
	private final JLabel avaLabel    = new JLabel("Available");
	private final JLabel lblAvailableQFilter = new JLabel("QuickFilter:");
	private final JLabel lblSelectedQFilter  = new JLabel("QuickFilter:");
	private final JLabel goldLabel   = new JLabel(Globals.getLongCurrencyDisplay() + ": ");
	private final JLabel lblBuyRate  = new JLabel("Buy percentage:");
	private final JLabel lblSellRate = new JLabel("Sell percentage:");
	private final JLabel selLabel    = new JLabel("Selected");
	private final JLabel valueLabel  = new JLabel("Total Value: ");
	private JButton addButton;
	private JButton removeButton;
	private JButton clearAvailableQFilterButton = new JButton("Clear");
	private JButton clearSelectedQFilterButton = new JButton("Clear");
	private JButton setAvailableQFilterButton = new JButton("Set");
	private JButton setSelectedQFilterButton = new JButton("Set");
	private JCheckBox allowDebtBox = new JCheckBox("Allow Debt");
	private JCheckBox autoResize   = new JCheckBox("Auto Resize");
	private JCheckBox autoSort     = new JCheckBox("Auto-sort output", true);
	private JCheckBox costBox      = new JCheckBox("Ignore Cost");
	private JComboBoxEx viewComboBox = new JComboBoxEx();
	private JComboBoxEx viewSelectComboBox = new JComboBoxEx();
	private JLabelPane infoLabel = new JLabelPane();
	private JMenu pcCopyMenu = Utility.createMenu("Copy Item To", (char) 0, "Copy Item To", null, true);
	private JMenu pcMoveMenu = Utility.createMenu("Move Item To", (char) 0, "Move Item To", null, true);
	private JPanel center  = new JPanel();
	private JPanel pnlBuy  = new JPanel();
	private JPanel pnlSell = new JPanel();
	private JPanel south   = new JPanel();
	private JScrollPane eqScroll = new JScrollPane();
	private JScrollPane scrollPane;
	private JTextField textAvailableQFilter = new JTextField();
	private JTextField textSelectedQFilter = new JTextField();
	private JTextField gold = new JTextField();
	private JTextField totalValue = new JTextField("Temp");
	private JTreeTable availableTable; // the available Equipment
	private JTreeTable selectedTable; // the selected Equipment
	private JTreeTableSorter availableSort = null;
	private JTreeTableSorter selectedSort = null;
	private SellGearActionListener sellOneListener = new SellGearActionListener(1);
	private Runnable sellOneRunnable = new Runnable()
		{
			public void run()
			{
				sellOneListener.actionPerformed(null);
			}
		};

	private TreePath selPath;
	private boolean hasBeenSized = false;
	private boolean readyForRefresh = false;

//	private static final int COL_CARRIED = 4;
//	private static final int EQUIPMENT_NOTCARRIED = 0;
//	private static final int EQUIPMENT_CARRIED = 1;
//	private static final int EQUIPMENT_EQUIPPED = 2;
//	private static final int EQUIPMENT_CONTAINED = 3;
	// Right-click inventory item
	
	private PlayerCharacter pc;
	private int serial = 0;

	/**
	 * Constructor
	 * @param pc
	 */
	public InfoGear(PlayerCharacter pc)
	{
		this.pc = pc;
		// we use the component's name to save
		// component specific settings
		setName(Constants.tabNames[Constants.TAB_GEAR]);

		SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					initComponents();
					initActionListeners();
				}
			});
	}

	public void setPc(PlayerCharacter pc)
	{
		if(this.pc != pc || pc.getSerial() > serial)
		{
			this.pc = pc;
			serial = pc.getSerial();
			forceRefresh();
		}
	}

	public PlayerCharacter getPc()
	{
		return pc;
	}

	public int getTabOrder()
	{
		return SettingsHandler.getPCGenOption(".Panel.Gear.Order", Constants.TAB_GEAR);
	}

	public void setTabOrder(int order)
	{
		SettingsHandler.setPCGenOption(".Panel.Gear.Order", order);
	}

	public String getTabName()
	{
		GameMode game = SettingsHandler.getGame();
		return game.getTabName(Constants.TAB_GEAR);
	}

	public boolean isShown()
	{
		GameMode game = SettingsHandler.getGame();
		return game.getTabShown(Constants.TAB_GEAR);
	}

	/**
	 * Retrieve the list of tasks to be done on the tab.
	 * @return List of task descriptions as Strings.
	 */
	public List getToDos()
	{
		List toDoList = new ArrayList();
		return toDoList;
	}

	public void refresh()
	{
		if(pc.getSerial() > serial)
		{
			serial = pc.getSerial();
			forceRefresh();
		}
	}

	public void forceRefresh()
	{
		if(readyForRefresh)
		{
			needsUpdate = true;
			updateCharacterInfo();
		}
		else
		{
			serial = 0;
		}
	}

	public JComponent getView()
	{
		return this;
	}

	/**
	 * Set the needs update flag
	 * @param flag
	 */
	public static void setNeedsUpdate(boolean flag)
	{
		needsUpdate = flag;
	}

	/**
	 * specifies whether the "match any" option should be available
	 * @return true
	 */
	public boolean isMatchAnyEnabled()
	{
		return true;
	}

	/**
	 * specifies whether the "negate/reverse" option should be available
	 * @return true
	 */
	public boolean isNegateEnabled()
	{
		return true;
	}

	/**
	 * specifies the filter selection mode
	 * @return FilterConstants.MULTI_MULTI_MODE = 2
	 */
	public int getSelectionMode()
	{
		return FilterConstants.MULTI_MULTI_MODE;
	}

	/**
	 * This method causes the character to buy a number of the
	 * given equipment item at the rate specified by the user
	 *
	 * @param selectedEquipment Equipment to sell
	 * @param newQty double number of the item to buy
	 **/
	public void buySpecifiedEquipment(Equipment selectedEquipment, double newQty)
	{
		buySpecifiedEquipmentRate(selectedEquipment, newQty, SettingsHandler.getGearTab_BuyRate());
	}

	/**
	 * implementation of Filterable interface
	 */
	public void initializeFilters()
	{
		FilterFactory.registerAllSourceFilters(this);
		FilterFactory.registerAllSizeFilters(this);
		FilterFactory.registerAllEquipmentFilters(this);

		setKitFilter("GEAR");
	}

	/**
	 * Refresh the available gear list
	 * @param newEq
	 * @param purchase
	 * @param isCurrent
	 */
	public void refreshAvailableList(Equipment newEq, boolean purchase, boolean isCurrent)
	{
		// Add new item to available list
		availableModel.addItemToModel(newEq, true);
		availableModel.updateTree();
		availableTable.updateUI();

		// select the item just added
		if (isCurrent)
		{
			if (availableTable.search(newEq.getName(), true) != null)
			{
				availableTable.requestFocus();
			}
		}

		// Attempt to purchase if that was requested
		if (purchase)
		{
			buySpecifiedEquipment(newEq, 1);
		}

		// TODO: need to resync the selected list to add any types that have just been added
	}

	/**
	 * implementation of Filterable interface
	 */
	public void refreshFiltering()
	{
		forceRefresh();
	}

	/**
	 * This method causes the character to sell a number of the
	 * given equipment item from inventory at the rate specified
	 * in the rate combo box.
	 * @param selectedEquipment Equipment to sell
	 * @param qty double number of the item to sell
	 **/
	public void sellSpecifiedEquipment(Equipment selectedEquipment, double qty)
	{
		sellSpecifiedEquipmentRate(selectedEquipment, qty, SettingsHandler.getGearTab_SellRate());
	}

	// This recalculates the states of everything based upon the currently selected
	// character.
	private void updateCharacterInfo()
	{
		int ix = 0;
		pcMoveMenu.removeAll();
		pcCopyMenu.removeAll();

		for (Iterator i = Globals.getPCList().iterator(); i.hasNext();)
		{
			PlayerCharacter testPc = (PlayerCharacter) i.next();

			if (testPc != pc)
			{
				pcMoveMenu.add(Utility.createMenuItem(testPc.getName(), new MoveItemListener(ix, 0), "MoveItemTo",
						(char) 0, null, "Move Item To " + testPc.getName(), null, true));
				pcCopyMenu.add(Utility.createMenuItem(testPc.getName(), new MoveItemListener(ix, 1), "CopyItemTo",
						(char) 0, null, "Copy Item To " + testPc.getName(), null, true));
			}

			++ix;
		}

		// Only make the menu usable if there is something there!
		pcMoveMenu.setEnabled(pcMoveMenu.getItemCount() > 0);
		pcCopyMenu.setEnabled(pcCopyMenu.getItemCount() > 0);

		if (!needsUpdate)
		{
			return;
		}

		autoSort.setSelected(pc.isAutoSortGear());
		pc.aggregateFeatList();
		updateAvailableModel();
		updateSelectedModel();
		gold.setText(pc.getGold().toString());
		updateTotalValue();
		needsUpdate = false;
	}

	private static int getEventSelectedIndex(ListSelectionEvent e)
	{
		final DefaultListSelectionModel model = (DefaultListSelectionModel) e.getSource();

		if (model == null)
		{
			return -1;
		}

		return model.getMinSelectionIndex();
	}

	/**
	 * This method gets the Object currently selected in the Available gear
	 * treetable.
	 * @return Object selected in the table or null if no item can be located
	 */
	private Object getCurrentAvailableTableItem()
	{
		Object item = null;
		final int row = availableTable.getSelectionModel().getAnchorSelectionIndex();

		if (row >= 0)
		{
			final TreePath treePath = availableTable.getTree().getPathForRow(row);
			final Object eo = treePath.getLastPathComponent();
			final PObjectNode e = (PObjectNode) eo;
			item = e.getItem();
		}

		return item;
	}

	/**
	 * This method gets the Object currently selected in the Available gear
	 * treetable.
	 * @return Object selected in the table or null if no item can be located
	 */
	private Object getCurrentSelectedTableItem()
	{
		Object item = null;
		final int row = selectedTable.getSelectionModel().getAnchorSelectionIndex();

		if (row >= 0)
		{
			final TreePath treePath = selectedTable.getTree().getPathForRow(row);
			final Object eo = treePath.getLastPathComponent();
			final PObjectNode e = (PObjectNode) eo;
			item = e.getItem();
		}

		return item;
	}

	/**
	 * Retrieve the highest output index used in any of the
	 * character's equipment.
	 * @return highest output index
	 */
	private int getHighestOutputIndex()
	{
		int maxOutputIndex = 0;

		if (pc == null)
		{
			return 0;
		}

		for (Iterator i = pc.getEquipmentMasterList().iterator(); i.hasNext();)
		{
			final Equipment item = (Equipment) i.next();

			if (item.getOutputIndex() > maxOutputIndex)
			{
				maxOutputIndex = item.getOutputIndex();
			}
		}

		return maxOutputIndex;
	}

	private void setInfoLabelText(Equipment aEq)
	{
		if (aEq != null)
		{
			StringBuffer b = new StringBuffer(300);
			b.append("<html><b>").append(aEq.piSubString()).append("</b>");

			if (!aEq.longName().equals(aEq.getName()))
			{
				b.append("(").append(aEq.longName()).append(")");
			}

			b.append(" &nbsp;<b>TYPE</b>:").append(aEq.getTypeUsingFlag(true));

			//
			// Should only be meaningful for weapons, but if included on some other piece of
			// equipment, show it anyway
			//
			if (aEq.hasWield())
			{
				WieldCategory wCat = Globals.effectiveWieldCategory(pc, aEq);
				if (wCat != null)
				{
					b.append(" <b>Wield:</b> ").append(wCat.getName());
				}
			}

			//
			// Only meaningful for weapons, armor and shields
			//
			if (aEq.isWeapon() || aEq.isArmor() || aEq.isShield())
			{
				b.append(" <b>PROFICIENT</b>:");
				b.append(((pc.isProficientWith(aEq) && aEq.meetsPreReqs(pc)) ? "Y"
																										  : (SettingsHandler
					.getPrereqFailColorAsHtmlStart() + "N" + SettingsHandler
					.getPrereqFailColorAsHtmlEnd())));
			}

			final String cString = aEq.preReqHTMLStrings(pc, false);

			if (cString.length() > 0)
			{
				b.append(" &nbsp;<b>Requirements</b>:").append(cString);
			}

			String IDS = aEq.getInterestingDisplayString(pc);

			if (IDS.length() > 0)
			{
				b.append(" &nbsp;<b>Properties</b>:").append(IDS);
			}

			String bString = Globals.getGameModeUnitSet().displayWeightInUnitSet(aEq.getWeight(pc).doubleValue());

			if (bString.length() > 0)
			{
				b.append(" <b>WT</b>:").append(bString).append(Globals.getGameModeUnitSet().getWeightUnit());
			}

			Integer a = aEq.getMaxDex(pc);

			if (a.intValue() != 100)
			{
				b.append(" <b>MAXDEX</b>:").append(a.toString());
			}

			a = aEq.acCheck(pc);

			if (aEq.isArmor() || aEq.isShield() || (a.intValue() != 0))
			{
				b.append(" <b>ACCHECK</b>:").append(a.toString());
			}

			if (Globals.getGameModeACText().length() != 0)
			{
				a = aEq.getACBonus(pc);

				if (aEq.isArmor() || aEq.isShield() || (a.intValue() != 0))
				{
					b.append(" <b>").append(Globals.getGameModeACText()).append(" Bonus</b>:").append(a.toString());
				}
			}

			if (Globals.getGameModeShowSpellTab())
			{
				a = aEq.spellFailure(pc);

				if (aEq.isArmor() || aEq.isShield() || (a.intValue() != 0))
				{
					b.append(" <b>Arcane Failure</b>:").append(a.toString());
				}
			}

			bString = Globals.getGameModeDamageResistanceText();

			if (bString.length() != 0)
			{
				a = aEq.eDR(pc);

				if (aEq.isArmor() || aEq.isShield() || (a.intValue() != 0))
				{
					b.append(" <b>").append(bString).append("</b>:").append(a.toString());
				}
			}

			bString = aEq.moveString();

			if (bString.length() > 0)
			{
				b.append(" <b>Move</b>:").append(bString);
			}

			bString = aEq.getSize();

			if (bString.length() > 0)
			{
				b.append(" <b>Size</b>:").append(bString);
			}

			bString = aEq.getDamage(pc);

			if (bString.length() > 0)
			{
				b.append(" <b>Damage</b>:").append(bString);

				if (aEq.isDouble())
				{
					b.append('/').append(aEq.getAltDamage(pc));
				}
			}

			bString = aEq.getCritRange(pc);

			if (bString.length() > 0)
			{
				b.append(" <b>Crit Range</b>:").append(bString);

				if (aEq.isDouble() && !aEq.getCritRange(pc).equals(aEq.getAltCritRange(pc)))
				{
					b.append('/').append(aEq.getAltCritRange(pc));
				}
			}

			bString = aEq.getCritMult();

			if (bString.length() > 0)
			{
				b.append(" <b>Crit Mult</b>:").append(bString);

				if (aEq.isDouble() && !aEq.getCritMult().equals(aEq.getAltCritMult()))
				{
					b.append('/').append(aEq.getAltCritMult());
				}
			}

			if (aEq.isWeapon())
			{
				bString = Globals.getGameModeUnitSet().displayDistanceInUnitSet(aEq.getRange(pc).intValue());

				if (bString.length() > 0)
				{
					b.append(" <b>Range</b>:").append(bString).append(Globals.getGameModeUnitSet().getDistanceUnit());
				}
			}

			bString = aEq.getContainerCapacityString();

			if (bString.length() > 0)
			{
				b.append(" <b>Container</b>:").append(bString);
			}

			bString = aEq.getContainerContentsString();

			if (bString.length() > 0)
			{
				b.append(" <b>Currently Contains</b>:").append(bString);
			}

			final int charges = aEq.getRemainingCharges();

			if (charges >= 0)
			{
				b.append(" <b>Charges</b>:").append(charges);
			}

			bString = aEq.getSource();

			if (bString.length() > 0)
			{
				b.append(" <b>SOURCE</b>:").append(bString);
			}

			b.append("</html>");
			infoLabel.setText(b.toString());
		}
		else
		{
			infoLabel.setText();
		}
	}

	/**
	 * This method performs the actual adjustment of the character's
	 * equipment
	 * 
	 * Do NOT modify the equipment set directly or from any method other than this one
	 * 
	 * Method overhauled March, 2003 by sage_sam as part of FREQ 606205
	 *
	 * @param equipItemToAdjust Equipment item selected to update
	 * @param adjustment number of items to add (positive) or
	 * remove (negative) from the character.
	 * @return double containing the actual adjustment made to a character,
	 * i.e. if 10 were requested removed but the charater only had 5,
	 * this will return 5.
	 */
	private double adjustBelongings(final Equipment equipItemToAdjust, final double adjustment)
	{
		int nextOutputIndex = 1;
		double actualAdjustment = adjustment;

		if (pc != null)
		{
			Equipment updatedItem = pc.getEquipmentNamed(equipItemToAdjust.getName());

			// see if item is already in inventory; update it
			if (updatedItem != null)
			{
				final double prevQty = (updatedItem.qty() < 0) ? 0 : updatedItem.qty();
				final double newQty = prevQty + adjustment;

				final double numberOfItemInUse = getNumberOfItemInUse(pc, updatedItem);
				if (newQty <= 0)
				{
					// completely remove item
					if (numberOfItemInUse>0.0) {
						ShowMessageDelegate.showMessageDialog("You can not remove all of '"+updatedItem.getName() + "' as it is still present in at least one equipment set.", "Error", MessageType.ERROR);
						return 0.0;
					}


					actualAdjustment = -prevQty;
					updatedItem.setNumberCarried(new Float(0));
					updatedItem.setLocation(Equipment.NOT_CARRIED);

					final Equipment eqParent = (Equipment) updatedItem.getParent();

					if (eqParent != null)
					{
						eqParent.removeChild(pc, updatedItem);
					}

					pc.removeEquipment(updatedItem);
					pc.delEquipSetItem(updatedItem);
					selectedModel.removeItemFromNodes(null, updatedItem);
					pc.setCalcEquipmentList();
					pc.totalWeight();
				}
				else
				{
					// update item count
					if (numberOfItemInUse>newQty) {
						ShowMessageDelegate.showMessageDialog("You can not set the total number of '"+updatedItem.getName() + "' to be "+newQty+" as there is an Equipment Set that is using "+numberOfItemInUse + " of them.", "Error", MessageType.ERROR);
						return 0.0;
					}
					Float qty = new Float(newQty);
					updatedItem.setQty(qty);
					updatedItem.setNumberCarried(qty);
					selectedModel.setValueForItemInNodes(null, updatedItem, newQty, COL_QTY_SRC);
				}
			}
			else // item is not in inventory; add it
			{
				if (adjustment > 0)
				{
					updatedItem = (Equipment) equipItemToAdjust.clone();

					if (updatedItem != null)
					{
						// Calc the item's output order
						if (autoSort.isSelected())
						{
							updatedItem.setOutputIndex(nextOutputIndex);
							resortSelected(ResortComparator.RESORT_NAME, ResortComparator.RESORT_ASCENDING);
						}
						else
						{
							if (updatedItem.getOutputIndex() == 0)
							{
								updatedItem.setOutputIndex(getHighestOutputIndex() + 1);
							}
						}

						// Set the number carried and add it to the character
						Float qty = new Float(adjustment);
						updatedItem.setQty(qty);
						updatedItem.setNumberCarried(qty);
						pc.addEquipment(updatedItem);

						// Update the selected table
						selectedModel.addItemToModel(updatedItem, true);
					}
				}
			}

			// Update the PC and equipment
			pc.setCalcEquipmentList();
			updateEqInfo(updatedItem);
			pc.setDirty(true);

			// Update the selected table
			selectedModel.updateTree();
			selectedTable.updateUI();

			// Return the actual adjustment amount
			return actualAdjustment;
		}

		return 0;
	}

	/**
	 * Returns the number of these objects that have been allocated to an
	 * equipment set.
	 *
	 * i.e. If the character has 10 throwing axes and has assigned 4 to Primary
	 * Hand in EqSet 1, and 6 to Backpack and 1 to Primary Hand in EqSet 2 then
	 * this method will return 7
	 *
	 * @param pc2
	 *            The Character to examine
	 * @param equipment
	 *            The piece of equipment to look for in all of the equipmentSets
	 * @return The number of times this piece of equipment has been allocated in
	 *         a single EqSet
	 */
	private double getNumberOfItemInUse(PlayerCharacter pc2, Equipment equipment) {
		Map foundCounts = new HashMap();
		for (Iterator iter = pc2.getEquipSet().iterator(); iter.hasNext();) {
			EquipSet element = (EquipSet) iter.next();

			if (element.getValue().equalsIgnoreCase(equipment.getName())) {
				String path = element.getRootIdPath();
				if (!foundCounts.containsKey(path)) {
					foundCounts.put(path, new Float(0.0));
				}
				Float count = (Float)foundCounts.get(path);
				count = new Float(count.floatValue()+element.getQty().floatValue());
				foundCounts.put(path, count);
			}
		}
		if (foundCounts.size()==0) {
			return 0.0;
		}
		Float max = (Float) Collections.max(foundCounts.values());
		return max.doubleValue();
	}

	/**
	 * This method adjusts the character's gold in the event an item is bought or sold.
	 * @param base Equipment item being bought/sold, used to determine the base price
	 * @param diffQty double number of the item bought/sold
	 * @param buyRate int rate (typically 0-100) at which to buy an item
	 * @param sellRate int rate (typically 0-100) at which to sell an item
	 *
	 * This method was overhauled March, 2003 by sage_sam as part of FREQ 606205
	 */
	private void adjustGold(Equipment base, double diffQty, int buyRate, int sellRate)
	{
		if (!costBox.isSelected() && (pc != null))
		{
			double itemCost = diffQty * base.getCost(pc).floatValue() * -0.01;

			if (diffQty < 0)
			{
				itemCost *= sellRate;
			}
			else if (diffQty > 0)
			{
				itemCost *= buyRate;
			}

			pc.setDirty(true);
			pc.adjustGold(itemCost);
			gold.setText(pc.getGold().toString());
		}
	}

	/**
	 * This method causes the character to buy a number of the
	 * given equipment item at the rate specified in rateCombo box
	 *
	 * @param selectedEquipment Equipment to buy
	 * @param qty double number of the item to buy
	 **/
	private void buySpecifiedEquipmentRate(Equipment selectedEquipment, double qty)
	{
		Object defaultValue = cmbSellPercent.getSelectedItem().toString();
		InputInterface ii = InputFactory.getInputInstance();
		Object input = ii.showInputDialog(this, "Enter buy price percentage:", "Buy at Percent",
				MessageType.QUESTION, null, defaultValue);

		if (input != null)
		{
			String iString = input.toString().trim();

			try
			{
				int buyRate = Integer.parseInt(iString);
				buySpecifiedEquipmentRate(selectedEquipment, qty, buyRate);
			}
			catch (NumberFormatException nfe)
			{
				ShowMessageDelegate.showMessageDialog("You must enter an integer value.", "Error", MessageType.ERROR);
			}
		}
	}

	/**
	 * This method causes the character to buy a number of the
	 * given equipment item at the rate specified.
	 * @param selectedEquipment Equipment to sell
	 * @param qtyToBuy double number of the item to buy
	 * @param buyRate int rate percentage (typically 0 to 100) at which to buy the item
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private void buySpecifiedEquipmentRate(Equipment selectedEquipment, double qtyToBuy, int buyRate)
	{
		try
		{
			if (selectedEquipment.getModifiersRequired())
			{
				if ((selectedEquipment.getEqModifierList(true).size() == 0)
					&& (selectedEquipment.getEqModifierList(false).size() == 0))
				{
					ShowMessageDelegate.showMessageDialog("You cannot buy this item as is; you must \"customize\" it first.",
						Constants.s_APPNAME, MessageType.ERROR);

					return;
				}
			}

			//
			// Get a number from the user via a popup
			//
			double buyQty = qtyToBuy;

			if (buyQty < 0)
			{
				Object selectedValue = JOptionPane.showInputDialog(null, "Enter Quantity", Constants.s_APPNAME,
						JOptionPane.QUESTION_MESSAGE);

				if (selectedValue != null)
				{
					try
					{
						buyQty = Float.parseFloat(((String) selectedValue).trim());
					}
					catch (Exception e)
					{
						ShowMessageDelegate.showMessageDialog("Invalid number!", Constants.s_APPNAME, MessageType.ERROR);

						return;
					}
				}
				else
				{
					return;
				}
			}

			if (selectedEquipment.acceptsChildren() && !CoreUtility.doublesEqual((buyQty % 1), 0))
			{
				ShowMessageDelegate.showMessageDialog(
					"You cannot buy, own or carry non-integral numbers of containers\n" +
					"i.e. Half a sack is nonsensical.",
					Constants.s_APPNAME,
					MessageType.ERROR);

				return;
			}

			if (	autoResize.isEnabled() && autoResize.isSelected() &&
					Globals.canResizeHaveEffect(pc, selectedEquipment, null) &&
					pc.sizeInt() != Globals.sizeInt(selectedEquipment.getSize()))
			{
				final String newSize     = pc.getSize();
				final String existingKey = selectedEquipment.getKeyName();
				final String newKey      = selectedEquipment.createKeyForAutoResize(newSize);

				Equipment potential = EquipmentList.getEquipmentKeyed(newKey);

				if (newKey.equals(existingKey))
				{
					// nothing to do
				}

				// If we've already resized this piece of equipment to this size
				// on a previous occasion, just substitute that piece of equipment
				// in place of the selected equipment.
				else if (potential != null)
				{
					selectedEquipment = potential;
				}
				else
				{
					final String newName = selectedEquipment.createNameForAutoResize(newSize);
					potential            = EquipmentList.getEquipmentNamed(newName);

					if (potential != null)
					{
						selectedEquipment = potential;
					}
					else
					{
						final String existingName = selectedEquipment.getName();
						final Equipment newEq     = (Equipment) selectedEquipment.clone();

						// This may seem insane, but if the base item is not set,
						// getBaseItemName returns the result of getName
						if (newEq.getBaseItemName().equals(existingName))
						{
							newEq.setBaseItem(existingName);
						}

						newEq.setName(newName);
						newEq.setOutputName(newName);
						newEq.setKeyName(newKey);
						newEq.resizeItem(pc, newSize);
						newEq.removeType("AUTO_GEN");
						newEq.removeType("STANDARD");
						if (!newEq.isType(Constants.s_CUSTOM)) {
							newEq.addMyType(Constants.s_CUSTOM);
						}

						EquipmentList.addEquipment(newEq);
						refreshAvailableList(newEq, false, true);

						selectedEquipment = newEq;
					}
				}
			}

			if (canAfford(selectedEquipment, buyQty, buyRate))
			{
				// Adjust the character's belongings
				adjustBelongings(selectedEquipment, buyQty);

				// Adjust the characters money
				adjustGold(selectedEquipment, buyQty, buyRate, 0);
			}
			else
			{
				ShowMessageDelegate.showMessageDialog(
						"Insufficient funds for purchase of " +
						qtyToBuy + " " + selectedEquipment.getName(),
						Constants.s_APPNAME,
						MessageType.INFORMATION);
			}
		}
		catch (Exception exc)
		{
			ShowMessageDelegate.showMessageDialog("buySpecifiedEquipment: Exception:" + exc.getMessage(), Constants.s_APPNAME,
				MessageType.ERROR);
		}
	}

	/**
	 * This method is called to determine whether the character can afford to buy
	 * the requested quantity of an item at the rate selected.
	 * @param selected Equipment item being bought, used to determine the base price
	 * @param purchaseQty double number of the item bought
	 * @param buyRate int rate (typically 0-100) at which to buy an item
	 *
	 * This method was overhauled March, 2003 by sage_sam as part of FREQ 606205
	 * @return true if it can be afforded
	 */
	private boolean canAfford(Equipment selected, double purchaseQty, int buyRate)
	{
		final float currentFunds = ((pc != null) ? pc.getGold().floatValue() : 0);

		final double itemCost = (purchaseQty * buyRate) * (float) 0.01 * selected.getCost(pc).floatValue();

		return (costBox.isSelected() || allowDebtBox.isSelected() || (itemCost <= currentFunds));
	}

	private void createAvailableModel()
	{
		if (availableModel == null)
		{
			availableModel = new EquipmentModel(viewMode, true);
		}
		else
		{
			availableModel.resetModel(viewMode, true);
		}

		if (availableSort != null)
		{
			availableSort.setRoot((PObjectNode) availableModel.getRoot());
			availableSort.sortNodeOnColumn();
		}
	}

	/**
	 * Creates the EquipmentModel that will be used.
	 **/
	private void createModels()
	{
		createAvailableModel();
		createSelectedModel();
	}

	private void createSelectedModel()
	{
		if (selectedModel == null)
		{
			selectedModel = new EquipmentModel(viewSelectMode, false);
		}
		else
		{
			selectedModel.resetModel(viewSelectMode, false);
		}

		if (selectedSort != null)
		{
			selectedSort.setRoot((PObjectNode) selectedModel.getRoot());
			selectedSort.sortNodeOnColumn();
		}
	}

	private class AvailableClickHandler implements ClickHandler
	{
		public void singleClickEvent() {
			// Do Nothing
		}
		
		public void doubleClickEvent()
		{
			buyOneListener.actionPerformed(null);
		}
		public boolean isSelectable(Object obj)
		{
			return !(obj instanceof String);
		}
	}

	private class SelectedClickHandler implements ClickHandler
	{
		public void singleClickEvent() {
			// Do Nothing
		}
		
		public void doubleClickEvent()
		{
			SwingUtilities.invokeLater(sellOneRunnable);
		}
		public boolean isSelectable(Object obj)
		{
			return !(obj instanceof String);
		}
	}

	private void createTreeTables()
	{
		availableTable = new JTreeTable(availableModel);

		final JTree avaTree = availableTable.getTree();
		avaTree.setRootVisible(false);
		avaTree.setShowsRootHandles(true);
		avaTree.setCellRenderer(new LabelTreeCellRenderer());
		availableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		availableTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
			{
				public void valueChanged(ListSelectionEvent e)
				{
					if (!e.getValueIsAdjusting())
					{
						//final Object temp = availableTable.getTree().getLastSelectedPathComponent();
						final int idx = getEventSelectedIndex(e);

						if (idx < 0)
						{
							return;
						}

						final Object temp = availableTable.getTree().getPathForRow(idx).getLastPathComponent();
						Equipment aEq;

						if (temp != null)
						{
							//
							// Only display information about equipment, not equipment with
							// same name as type
							//
							final PObjectNode pobjn = (PObjectNode) temp;

							if (!(pobjn.getItem() instanceof Equipment))
							{
								setInfoLabelText(null);

								return;
							}

							aEq = (Equipment) pobjn.getItem();
						}
						else
						{
							// This will popup if displaying by name only and we add an item to the available list
							// after customizing, so I've removed it---Byngl
							//
							//GuiFacade.showMessageDialog(null,
							//	"No equipment selected! Try again.", Constants.s_APPNAME, GuiFacade.ERROR_MESSAGE);
							return;
						}

						addButton.setEnabled(aEq != null);
						setInfoLabelText(aEq);
					}
				}
			});

		availableTable.addMouseListener(new JTreeTableMouseAdapter(availableTable, new AvailableClickHandler(), false));

		selectedTable = new JTreeTable(selectedModel);

		selectedTable.getColumnModel().getColumn(COL_QTY_SRC).setCellEditor(new QuantityEditor());
		selectedTable.getColumnModel().getColumn(COL_INDEX).setCellEditor(new OutputOrderEditor(
				new String[]{ "First", "Last", "Hidden" }));

		final JTree selTree = selectedTable.getTree();
		selTree.setRootVisible(false);
		selTree.setShowsRootHandles(true);
		selTree.setCellRenderer(new LabelTreeCellRenderer());
		selectedTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
			{
				public void valueChanged(ListSelectionEvent e)
				{
					if (!e.getValueIsAdjusting())
					{
						Equipment aEq = null;
						Object temp = getCurrentSelectedTableItem();

						if (temp != null)
						{
							//
							// Only display information about equipment, not equipment with
							// same name as type
							if (!(temp instanceof Equipment))
							{
								setInfoLabelText(null);

								return;
							}

							aEq = (Equipment) temp;
						}
						else
						{
							infoLabel.setText();
						}

						removeButton.setEnabled(aEq != null);
						setInfoLabelText(aEq);
					}
				}
			});
		selectedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectedTable.addMouseListener(new JTreeTableMouseAdapter(selectedTable, new SelectedClickHandler(), false));

		hookupPopupMenu(availableTable);
		hookupPopupMenu(selectedTable);
	}

	private void customizeButtonClick()
	{
		if (!addButton.isEnabled())
		{
			return;
		}

		final int currentRow = availableTable.getSelectedRow();

		if (currentRow >= 0)
		{
			int row = availableTable.getSelectionModel().getAnchorSelectionIndex();
			TreePath treePath = availableTable.getTree().getPathForRow(row);
			Object eo = treePath.getLastPathComponent();
			PObjectNode e = (PObjectNode) eo;

			if (!(e.getItem() instanceof Equipment))
			{
				ShowMessageDelegate.showMessageDialog("Can only customise items, not types.", Constants.s_APPNAME, MessageType.ERROR);

				return;
			}

			final Equipment aEq = (Equipment) e.getItem();
			openCustomizer(aEq);
		}
	}

	private void deleteCustomButtonClick()
	{
		final int currentRow = availableTable.getSelectedRow();

		if (currentRow >= 0)
		{
			int row = availableTable.getSelectionModel().getAnchorSelectionIndex();
			TreePath treePath = availableTable.getTree().getPathForRow(row);
			Object eo = treePath.getLastPathComponent();
			PObjectNode e = (PObjectNode) eo;

			if (!(e.getItem() instanceof Equipment))
			{
				ShowMessageDelegate.showMessageDialog("Cannot delete types.", Constants.s_APPNAME, MessageType.ERROR);

				return;
			}

			Equipment aEq = (Equipment) e.getItem();

			if (!aEq.isType(Constants.s_CUSTOM))
			{
				ShowMessageDelegate.showMessageDialog("Can only delete custom items.", Constants.s_APPNAME, MessageType.ERROR);

				return;
			}

			List whoHasIt = new ArrayList();

			for (Iterator pcIterator = Globals.getPCList().iterator(); pcIterator.hasNext();)
			{
				final PlayerCharacter playerCharacter = (PlayerCharacter) pcIterator.next();

				if (playerCharacter.getEquipmentNamed(aEq.getName()) != null)
				{
					whoHasIt.add(playerCharacter.getName());
				}
			}

			if (whoHasIt.size() != 0)
			{
				String whose = whoHasIt.toString();
				whose = whose.substring(1, whose.length() - 1);
				ShowMessageDelegate.showMessageDialog("Can only delete items that are in no character's possession. "
				+ "The following character(s) have this item in their possession:\n" + whose,
					Constants.s_APPNAME, MessageType.ERROR);

				return;
			}

			aEq = EquipmentList.getEquipmentKeyed(aEq.getKeyName());

			if (aEq != null)
			{
				//
				// Give user a chance to bail
				//
				if (JOptionPane.showConfirmDialog(null, "Delete " + aEq.getName() + " from database?",
						Constants.s_APPNAME, JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
				{
					return;
				}

				EquipmentList.remove(aEq);

				//
				// This will unexpand all expanded nodes
				// TODO: be a little less draconian and remember what's expanded
				// TODO: sneak onto all other character's Gear tabs and refresh the available list
				updateAvailableModel();
			}
		}
	}

	private void editChargesButtonClicked()
	{
		Object item = getCurrentSelectedTableItem();

		if (!(item instanceof Equipment))
		{
			return;
		}

		final Equipment aEq = (Equipment) item;
		final int minCharges = aEq.getMinCharges();
		final int maxCharges = aEq.getMaxCharges();

		if (minCharges < 0)
		{
			ShowMessageDelegate.showMessageDialog("This item cannot hold charges.", Constants.s_APPNAME, MessageType.ERROR);

			return;
		}

		InputInterface ii = InputFactory.getInputInstance();
		Object selectedValue = ii.showInputDialog(null,
				"Enter Number of Charges (" + Integer.toString(minCharges) + "-" + Integer.toString(maxCharges) + ")",
				Constants.s_APPNAME, MessageType.INFORMATION, null,
				Integer.toString(aEq.getRemainingCharges()));

		if (selectedValue == null)
		{
			return;
		}

		try
		{
			final String aString = ((String) selectedValue).trim();
			int charges = Integer.parseInt(aString);

			if ((charges < minCharges) || (charges > maxCharges))
			{
				ShowMessageDelegate.showMessageDialog("Value out of range", Constants.s_APPNAME, MessageType.ERROR);

				return;
			}

			if (aEq.getRemainingCharges() != charges)
			{
				Equipment newEq = (Equipment) aEq.clone();
				newEq.setRemainingCharges(charges);
				pc.updateEquipSetItem(aEq, newEq);

				if (aEq.getQty().floatValue() <= 1.0f)
				{
					pc.removeEquipment(aEq);
				}

				pc.addEquipment(newEq);

				updateEqInfo(newEq);
				updateSelectedModel();
				pc.setDirty(true);

				return;
			}
		}
		catch (Exception exc)
		{
			// TODO This exception needs to be handled
		}
	}

	// This is called when the tab is shown.
	private void formComponentShown()
	{
		requestFocus();
		// TODO: I18N
		PCGen_Frame1.setMessageAreaTextWithoutSaving("Equipment character is not proficient with are in Red.");
		refresh();

		int s = splitPane.getDividerLocation();
		int t = bsplit.getDividerLocation();
		int u = asplit.getDividerLocation();
		int width;
		TableColumn[] acol = new TableColumn[NUM_COL_AVAILABLE];
		TableColumn[] scol = new TableColumn[NUM_COL_SELECTED];
		int[] awidth = new int[NUM_COL_AVAILABLE];
		int[] swidth = new int[NUM_COL_SELECTED];

		for (int i = 0; i < NUM_COL_AVAILABLE; i++)
		{
			acol[i] = availableTable.getColumnModel().getColumn(i);
			awidth[i] = acol[i].getWidth();
		}

		for (int i = 0; i < NUM_COL_SELECTED; i++)
		{
			scol[i] = selectedTable.getColumnModel().getColumn(i);
			swidth[i] = scol[i].getWidth();
		}

		if (!hasBeenSized)
		{
			hasBeenSized = true;
			s = SettingsHandler.getPCGenOption("InfoGear.splitPane", (int) ((this.getSize().getWidth() * 6) / 10));
			t = SettingsHandler.getPCGenOption("InfoGear.bsplit", (int) (this.getSize().getHeight() - 120));
			u = SettingsHandler.getPCGenOption("InfoGear.asplit", (int) (this.getSize().getWidth() - 295));

			// set the prefered width on selectedTable
			for (int i = 0; i < selectedTable.getColumnCount(); i++)
			{
				TableColumn sCol = selectedTable.getColumnModel().getColumn(i);
				width = Globals.getCustColumnWidth("InvSel", i);

				if (width == 0)
				{
					sCol.setPreferredWidth(swidth[i]);
				}
				else
				{
					sCol.setPreferredWidth(width);
				}

				sCol.addPropertyChangeListener(new ResizeColumnListener(selectedTable, "InvSel", i));
			}

			// set the prefered width on availableTable
			for (int i = 0; i < availableTable.getColumnCount(); i++)
			{
				TableColumn sCol = availableTable.getColumnModel().getColumn(i);
				width = Globals.getCustColumnWidth("InvAva", i);

				if (width == 0)
				{
					sCol.setPreferredWidth(swidth[i]);
				}
				else
				{
					sCol.setPreferredWidth(width);
				}

				sCol.addPropertyChangeListener(new ResizeColumnListener(availableTable, "InvAva", i));
			}
		}

		if (s > 0)
		{
			splitPane.setDividerLocation(s);
			SettingsHandler.setPCGenOption("InfoGear.splitPane", s);
		}

		if (t > 0)
		{
			bsplit.setDividerLocation(t);
			SettingsHandler.setPCGenOption("InfoGear.bsplit", t);
		}

		if (u > 0)
		{
			asplit.setDividerLocation(u);
			SettingsHandler.setPCGenOption("InfoGear.asplit", u);
		}
	}

	private void hookupPopupMenu(JTreeTable treeTable)
	{
		treeTable.addMouseListener(new GearPopupListener(treeTable, new GearPopupMenu(treeTable)));
	}

	private void initActionListeners()
	{
		gold.addFocusListener(new FocusAdapter()
			{
				public void focusLost(FocusEvent evt)
				{
					if (gold.getText().length() > 0)
					{
						if (pc != null)
						{
							pc.setDirty(true);
							pc.setGold(gold.getText());
						}
					}
					else if (pc != null)
					{
						gold.setText(pc.getGold().toString());
					}
				}
			});
		addComponentListener(new ComponentAdapter()
			{
				public void componentShown(ComponentEvent evt)
				{
					formComponentShown();
				}

				public void componentResized(ComponentEvent e)
				{
					bsplit.setDividerLocation((int) (InfoGear.this.getSize().getHeight() - 120));
					asplit.setDividerLocation((int) (InfoGear.this.getSize().getWidth() - 295));
				}
			});
		removeButton.addActionListener(sellOneListener);
		addButton.addActionListener(buyOneListener);
		viewComboBox.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					viewComboBoxActionPerformed();
				}
			});
		viewSelectComboBox.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					viewSelectComboBoxActionPerformed();
				}
			});
		textAvailableQFilter.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					setAvailableQFilter();
				}
			});
		setAvailableQFilterButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					setAvailableQFilter();
				}
			});
		clearAvailableQFilterButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					clearAvailableQFilter();
				}
			});
		textSelectedQFilter.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					setSelectedQFilter();
				}
			});
		setSelectedQFilterButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					setSelectedQFilter();
				}
			});
		clearSelectedQFilterButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					clearSelectedQFilter();
				}
			});
		costBox.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					SettingsHandler.setGearTab_IgnoreCost(costBox.isSelected());
				}
			});
		allowDebtBox.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					SettingsHandler.setGearTab_AllowDebt(allowDebtBox.isSelected());
				}
			});
		autoResize.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					SettingsHandler.setGearTab_AutoResize(autoResize.isSelected());
				}
			});
		autoSort.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					if (pc != null)
					{
						pc.setDirty(true);
						pc.setAutoSortGear(autoSort.isSelected());
					}
				}
			});

		FilterFactory.restoreFilterSettings(this);
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 **/
	private void initComponents()
	{
		readyForRefresh = true;
		//
		// Sanity check
		//
		int iView = SettingsHandler.getGearTab_AvailableListMode();

		if ((iView >= GuiConstants.INFOINVENTORY_VIEW_TYPE_SUBTYPE_NAME)
			&& (iView <= GuiConstants.INFOINVENTORY_VIEW_SOURCE_NAME))
		{
			viewMode = iView;
		}

		SettingsHandler.setGearTab_AvailableListMode(viewMode);
		iView = SettingsHandler.getGearTab_SelectedListMode();

		if ((iView >= GuiConstants.INFOINVENTORY_VIEW_TYPE_SUBTYPE_NAME)
			&& (iView <= GuiConstants.INFOINVENTORY_VIEW_SOURCE_NAME))
		{
			viewSelectMode = iView;
		}

		SettingsHandler.setGearTab_SelectedListMode(viewSelectMode);

		viewComboBox.addItem(PropertyFactory.getString("in_typeSubtypeName"));
		viewComboBox.addItem(PropertyFactory.getString("in_typeName"));
		viewComboBox.addItem(PropertyFactory.getString("in_nameLabel"));
		viewComboBox.addItem(PropertyFactory.getString("in_allTypes"));
		viewComboBox.addItem(PropertyFactory.getString("in_sourceName"));
		Utility.setDescription(viewComboBox, "You can change how the Equipment in the Tables are listed.");
		viewComboBox.setSelectedIndex(viewMode); // must be done before createModels call

		viewSelectComboBox.addItem(PropertyFactory.getString("in_typeSubtypeName"));
		viewSelectComboBox.addItem(PropertyFactory.getString("in_typeName"));
		viewSelectComboBox.addItem(PropertyFactory.getString("in_nameLabel"));
		viewSelectComboBox.addItem(PropertyFactory.getString("in_allTypes"));
		viewSelectComboBox.addItem(PropertyFactory.getString("in_sourceName"));
		Utility.setDescription(viewSelectComboBox, "You can change how the Equipment in the Tables are listed.");
		viewSelectComboBox.setSelectedIndex(viewSelectMode); // must be done before createModels call

		boolean customExists = Equipment.getEquipmentTypes().contains(Constants.s_CUSTOM);

		typeSubtypeRoot = new PObjectNode();
		typeRoot = new PObjectNode();
		allTypeRoot = new PObjectNode();
		sourceRoot = new PObjectNode();

		List aList = new ArrayList();
		List bList = new ArrayList();
		List sourceList = new ArrayList();

		if (customExists)
		{
			aList.add(Constants.s_CUSTOM);
			bList.add(Constants.s_CUSTOM);
			sourceList.add(Constants.s_CUSTOMSOURCE);
		}

		for (Iterator i = EquipmentList.getEquipmentListIterator(); i.hasNext(); )
		{
			Map.Entry entry = (Map.Entry)i.next();
			final Equipment bEq = (Equipment) entry.getValue();
			final StringTokenizer aTok = new StringTokenizer(bEq.getTypeUsingFlag(true), ".", false);

			// we only want the first TYPE to be in the top-level
			if (!aTok.hasMoreTokens())
			{
				continue;
			}

			String aString = aTok.nextToken();

			if (!aList.contains(aString))
			{
				aList.add(aString);
			}

			if (!bList.contains(aString))
			{
				bList.add(aString);
			}

			while (aTok.hasMoreTokens())
			{
				aString = aTok.nextToken();

				if (!bList.contains(aString))
				{
					bList.add(aString);
				}
			}

			String sourceString = bEq.getSourceWithKey("LONG");
			if ((sourceString != null) && (!sourceList.contains(sourceString)))
			{
				sourceList.add(sourceString);
			}
		}

		Collections.sort(aList);
		Collections.sort(bList);

		PObjectNode[] cc = new PObjectNode[aList.size()];
		PObjectNode[] dc = new PObjectNode[aList.size()];

		for (int i = 0; i < aList.size(); i++)
		{
			cc[i] = new PObjectNode();
			cc[i].setItem(aList.get(i).toString());
			cc[i].setParent((PObjectNode) typeSubtypeRoot);
			dc[i] = new PObjectNode();
			dc[i].setItem(aList.get(i).toString());
			dc[i].setParent((PObjectNode) typeRoot);
		}

		((PObjectNode) typeSubtypeRoot).setChildren(cc);
		((PObjectNode) typeRoot).setChildren(dc);

		Collections.sort(sourceList);
		PObjectNode[] pSources = new PObjectNode[sourceList.size()];
		for (int i = 0; i < sourceList.size(); i++)
		{
			pSources[i] = new PObjectNode();
			pSources[i].setItem(sourceList.get(i).toString());
			pSources[i].setParent((PObjectNode) sourceRoot);
		}
		((PObjectNode) sourceRoot).setChildren(pSources);

		for (int i = 0; i < cc.length; i++)
		{
			aList.clear();

			for (Iterator e = EquipmentList.getEquipmentListIterator(); e.hasNext(); ) {
				Map.Entry entry = (Map.Entry)e.next();
				final Equipment bEq = (Equipment) entry.getValue();
				final String topType = cc[i].toString();

				if (!bEq.isType(topType))
				{
					continue;
				}

				final StringTokenizer aTok = new StringTokenizer(bEq.getTypeUsingFlag(true), ".", false);

				//String aString = aTok.nextToken(); // skip first one, already in top-level
				while (aTok.hasMoreTokens())
				{
					final String aString = aTok.nextToken();

					if (!aString.equals(topType) && !aList.contains(aString))
					{
						aList.add(aString);
					}
				}
			}

			Collections.sort(aList);

			for (Iterator lI = aList.iterator(); lI.hasNext();)
			{
				String aString = (String) lI.next();
				PObjectNode d = new PObjectNode();
				d.setParent(cc[i]);
				cc[i].addChild(d);
				d.setItem(aString);
			}
		}

		PObjectNode[] ec = new PObjectNode[bList.size()];

		for (int i = 0; i < bList.size(); i++)
		{
			ec[i] = new PObjectNode();
			ec[i].setItem(bList.get(i).toString());
			ec[i].setParent((PObjectNode) allTypeRoot);
		}

		((PObjectNode) allTypeRoot).setChildren(ec);

		/*
		 * Setup GUI
		 */
		createModels();

		// create Treetables of equipment
		createTreeTables();

		center.setLayout(new BorderLayout());

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel leftPane = new JPanel();
		JPanel rightPane = new JPanel();
		leftPane.setLayout(gridbag);
		splitPane = new FlippingSplitPane(splitOrientation, leftPane, rightPane);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerSize(10);

		center.add(splitPane, BorderLayout.CENTER);

		Utility.buildConstraints(c, 0, 0, 4, 1, 100, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;

		JPanel aPanel = new JPanel();
		gridbag.setConstraints(aPanel, c);
		aPanel.add(avaLabel);
		aPanel.add(viewComboBox);

		ImageIcon newImage;
		newImage = IconUtilitities.getImageIcon("Forward16.gif");
		addButton = new JButton(newImage);
		Utility.setDescription(addButton, "Click to add the selected item from the Available list of equipment");
		addButton.setEnabled(false);
		aPanel.add(addButton);
		leftPane.add(aPanel);

		Utility.buildConstraints(c, 0, 1, 1, 1, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		gridbag.setConstraints(lblAvailableQFilter, c);
		leftPane.add(lblAvailableQFilter);
		
		Utility.buildConstraints(c, 1, 1, 1, 1, 95, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		gridbag.setConstraints(textAvailableQFilter, c);
		leftPane.add(textAvailableQFilter);
		
		Utility.buildConstraints(c, 2, 1, 1, 1, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		gridbag.setConstraints(setAvailableQFilterButton, c);
		setAvailableQFilterButton.setToolTipText("Set a Filter on the list of equipment below, e.g. 'sword'");
		leftPane.add(setAvailableQFilterButton);

		Utility.buildConstraints(c, 3, 1, 1, 1, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		gridbag.setConstraints(clearAvailableQFilterButton, c);
		clearAvailableQFilterButton.setEnabled(false);
		leftPane.add(clearAvailableQFilterButton);

		availableTable.setColAlign(COL_COST, SwingConstants.RIGHT);
		availableTable.getColumnModel().getColumn(COL_COST).setPreferredWidth(15);
		availableTable.setColAlign(COL_QTY_SRC, SwingConstants.LEFT);

		Utility.buildConstraints(c, 0, 2, 4, 1, 0, 95);
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		scrollPane = new JScrollPane(availableTable);
		gridbag.setConstraints(scrollPane, c);
		leftPane.add(scrollPane);

		gridbag = new GridBagLayout();
		c = new GridBagConstraints();
		rightPane.setLayout(gridbag);

		Utility.buildConstraints(c, 0, 0, 4, 1, 100, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		aPanel = new JPanel();
		gridbag.setConstraints(aPanel, c);
		aPanel.add(selLabel);
		aPanel.add(viewSelectComboBox);
		newImage = IconUtilitities.getImageIcon("Back16.gif");
		removeButton = new JButton(newImage);
		Utility.setDescription(removeButton, "Click to add the selected item from the Selected list of equipment");
		removeButton.setEnabled(false);
		aPanel.add(removeButton);
		aPanel.add(autoSort);
		rightPane.add(aPanel);

		Utility.buildConstraints(c, 0, 1, 1, 1, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		gridbag.setConstraints(lblSelectedQFilter, c);
		rightPane.add(lblSelectedQFilter);
		
		Utility.buildConstraints(c, 1, 1, 1, 1, 95, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		gridbag.setConstraints(textSelectedQFilter, c);
		rightPane.add(textSelectedQFilter);
		
		Utility.buildConstraints(c, 2, 1, 1, 1, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		gridbag.setConstraints(setSelectedQFilterButton, c);
		setSelectedQFilterButton.setToolTipText("Set a Filter on the list of equipment below, e.g. 'sword'");
		rightPane.add(setSelectedQFilterButton);

		Utility.buildConstraints(c, 3, 1, 1, 1, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		gridbag.setConstraints(clearSelectedQFilterButton, c);
		clearSelectedQFilterButton.setEnabled(false);
		rightPane.add(clearSelectedQFilterButton);

		selectedTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(60);
		selectedTable.setColAlign(COL_COST, SwingConstants.RIGHT);
		selectedTable.getColumnModel().getColumn(COL_COST).setPreferredWidth(15);
		selectedTable.setColAlign(COL_QTY_SRC, SwingConstants.CENTER);
		selectedTable.getColumnModel().getColumn(COL_QTY_SRC).setPreferredWidth(10);
		selectedTable.getColumnModel().getColumn(COL_INDEX).setCellRenderer(new OutputOrderRenderer());
		selectedTable.getColumnModel().getColumn(COL_INDEX).setPreferredWidth(20);

		Utility.buildConstraints(c, 0, 2, 4, 1, 0, 90);
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		scrollPane = new JScrollPane(selectedTable);
		gridbag.setConstraints(scrollPane, c);
		rightPane.add(scrollPane);

		TitledBorder title1 = BorderFactory.createTitledBorder("Equipment Info");
		title1.setTitleJustification(TitledBorder.CENTER);
		eqScroll.setBorder(title1);
		infoLabel.setBackground(rightPane.getBackground());
		eqScroll.setViewportView(infoLabel);
		Utility.setDescription(eqScroll, "Any requirements you don't meet are in italics.");

		GridBagLayout gridbag2 = new GridBagLayout();
		GridBagConstraints c2 = new GridBagConstraints();
		south.setLayout(gridbag2);
		south.setMinimumSize(new Dimension(280, 66));

		totalValue.setEditable(false);
		totalValue.setColumns(8);
		totalValue.setBorder(null);
		totalValue.setOpaque(false);

		gold.setColumns(9);

		JPanel fPanel = new JPanel();
		fPanel.setLayout(new BorderLayout());

		JPanel f2Panel = new JPanel();
		f2Panel.setLayout(new BorderLayout());
		f2Panel.add(goldLabel, BorderLayout.WEST);
		f2Panel.add(gold, BorderLayout.EAST);
		fPanel.add(f2Panel, BorderLayout.EAST);
		Utility.buildConstraints(c2, 0, 0, 1, 1, 0, 0);
		c2.fill = GridBagConstraints.BOTH;
		c2.anchor = GridBagConstraints.WEST;
		gridbag2.setConstraints(fPanel, c2);
		south.add(fPanel);

		JPanel gPanel = new JPanel();
		gPanel.setLayout(new BorderLayout());

		JPanel g2Panel = new JPanel();
		g2Panel.setLayout(new BorderLayout());
		g2Panel.add(valueLabel, BorderLayout.WEST);
		g2Panel.add(totalValue, BorderLayout.EAST);
		gPanel.add(g2Panel, BorderLayout.EAST);
		Utility.buildConstraints(c2, 0, 1, 1, 1, 0, 0);
		c2.fill = GridBagConstraints.BOTH;
		c2.anchor = GridBagConstraints.WEST;
		gridbag2.setConstraints(gPanel, c2);
		south.add(gPanel);

		// Note this one section has been changed to use the new
		// preferred constant names of this layoutManager

		JPanel hPanel = new JPanel();
		hPanel.setLayout(new BorderLayout());
		hPanel.add(allowDebtBox, BorderLayout.LINE_START);
		hPanel.add(costBox, BorderLayout.LINE_END);
		hPanel.add(autoResize, BorderLayout.PAGE_END);

		Utility.buildConstraints(c2, 0, 2, 1, 1, 0, 0);
		c2.fill = GridBagConstraints.BOTH;
		c2.anchor = GridBagConstraints.WEST;
		gridbag2.setConstraints(hPanel, c2);
		south.add(hPanel);
		costBox.setSelected(SettingsHandler.getGearTab_IgnoreCost());
		autoResize.setSelected(SettingsHandler.getGearTab_AutoResize());
		autoResize.setEnabled(SettingsHandler.getGame().getAllowAutoResize());
		allowDebtBox.setSelected(SettingsHandler.getGearTab_AllowDebt());

		Integer[] predefinedPercent = new Integer[5];

		for (int i = 0; i < 5; ++i)
		{
			predefinedPercent[i] = new Integer(i * 50);
		}

		cmbBuyPercent.setEditable(true);
		cmbSellPercent.setEditable(true);
		cmbBuyPercent.setModel(new DefaultComboBoxModel(predefinedPercent));
		cmbSellPercent.setModel(new DefaultComboBoxModel(predefinedPercent));

		cmbBuyPercent.setSelectedItem(new Integer(SettingsHandler.getGearTab_BuyRate()));
		cmbSellPercent.setSelectedItem(new Integer(SettingsHandler.getGearTab_SellRate()));

		cmbBuyPercent.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					if (evt.getActionCommand().equals("comboBoxChanged"))
					{
						final Object enteredRate = cmbBuyPercent.getSelectedItem();
						int rate;

						if (enteredRate instanceof Integer)
						{
							rate = ((Integer) enteredRate).intValue();
						}
						else
						{
							try
							{
								rate = Integer.parseInt((String) enteredRate);
							}
							catch (Exception exc)
							{
								rate = -1;
							}
						}

						if (rate < 0)
						{
							rate = SettingsHandler.getGearTab_BuyRate();
							cmbBuyPercent.setSelectedItem(new Integer(rate));
						}

						SettingsHandler.setGearTab_BuyRate(rate);
					}
				}
			});
		cmbSellPercent.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					if (evt.getActionCommand().equals("comboBoxChanged"))
					{
						final Object enteredRate = cmbSellPercent.getSelectedItem();
						int rate;

						if (enteredRate instanceof Integer)
						{
							rate = ((Integer) enteredRate).intValue();
						}
						else
						{
							try
							{
								rate = Integer.parseInt((String) enteredRate);
							}
							catch (Exception exc)
							{
								rate = -1;
							}
						}

						if (rate < 0)
						{
							rate = SettingsHandler.getGearTab_SellRate();
							cmbSellPercent.setSelectedItem(new Integer(rate));
						}

						SettingsHandler.setGearTab_SellRate(rate);
					}
				}
			});

		pnlBuy.setLayout(new BorderLayout());
		pnlSell.setLayout(new BorderLayout());

		pnlBuy.add(lblBuyRate, BorderLayout.WEST);
		pnlBuy.add(cmbBuyPercent, BorderLayout.EAST);
		pnlSell.add(lblSellRate, BorderLayout.WEST);
		pnlSell.add(cmbSellPercent, BorderLayout.EAST);
		Utility.buildConstraints(c2, 0, 3, 1, 1, 0, 0);
		c2.fill = GridBagConstraints.BOTH;
		c2.anchor = GridBagConstraints.WEST;
		gridbag2.setConstraints(pnlBuy, c2);
		south.add(pnlBuy, c2);

		Utility.buildConstraints(c2, 0, 4, 1, 1, 0, 0);
		c2.fill = GridBagConstraints.BOTH;
		c2.anchor = GridBagConstraints.WEST;
		gridbag2.setConstraints(pnlSell, c2);
		south.add(pnlSell);

		asplit = new FlippingSplitPane(JSplitPane.HORIZONTAL_SPLIT, eqScroll, south);
		asplit.setOneTouchExpandable(true);
		asplit.setDividerSize(10);

		JPanel botPane = new JPanel();
		botPane.setLayout(new BorderLayout());
		botPane.setMinimumSize(new Dimension(200, 120));
		botPane.add(asplit, BorderLayout.CENTER);
		bsplit = new FlippingSplitPane(JSplitPane.VERTICAL_SPLIT, center, botPane);
		bsplit.setOneTouchExpandable(true);
		bsplit.setDividerSize(10);

		this.setLayout(new BorderLayout());
		this.add(bsplit, BorderLayout.CENTER);
		availableSort = new JTreeTableSorter(availableTable, (PObjectNode) availableModel.getRoot(), availableModel);
		selectedSort = new JTreeTableSorter(selectedTable, (PObjectNode) selectedModel.getRoot(), selectedModel);
		addFocusListener(new FocusAdapter()
			{
				public void focusGained(FocusEvent evt)
				{
					refresh();
				}
			});
	}

	private void openCustomizer(Equipment aEq)
	{
		if (aEq != null)
		{
			if (eqFrame == null)
			{
				eqFrame = new EQFrame(pc);
			}

			if (eqFrame.setEquipment(aEq))
			{
				eqFrame.setVisible(true);
				eqFrame.toFront();
			}
		}
	}

	/**
	 * This method removes a selected number of equipment items from inventory.
	 * If the number of requested items to remove is greater than the number in
	 * inventory, all items of the type given are removed.  If the number of requested
	 * items to remove is negative, the user is prompted for a number of items to remove.
	 * @param selectedEquipment Equipment item to remove
	 * @param qty double number of items to remove
	 * @return double number of items actually removed from the character's inventory
	 */
	private double removeSpecifiedEquipment(Equipment selectedEquipment, double qty)
	{
		// Get a number from the user via a popup
		double sellQty = qty;

		if (sellQty < 0.0f)
		{
			Object selectedValue = JOptionPane.showInputDialog(null, "Enter Quantity", Constants.s_APPNAME,
					JOptionPane.QUESTION_MESSAGE);

			if (selectedValue != null)
			{
				try
				{
					sellQty = Float.parseFloat(((String) selectedValue).trim());
				}
				catch (Exception e)
				{
					ShowMessageDelegate.showMessageDialog("Invalid number!", Constants.s_APPNAME, MessageType.ERROR);

					return 0;
				}
			}
			else
			{
				return 0;
			}
		}

		if (selectedEquipment.getChildCount() == 0)
		{
			if (!selectedEquipment.acceptsChildren() || (CoreUtility.doublesEqual((sellQty % 1), 0)))
			{
				return adjustBelongings(selectedEquipment, -sellQty);
			}
			ShowMessageDelegate.showMessageDialog("You cannot buy, own or carry non-integral numbers of containers\ni.e. Half a sack is nonsensical.",
				Constants.s_APPNAME,
				MessageType.ERROR);

			return 0;
		}
		ShowMessageDelegate.showMessageDialog("Cannot remove container unless it is empty.", Constants.s_APPNAME, MessageType.ERROR);
		return 0;
	}

	private void resortSelected(int sort, boolean sortOrder)
	{
		ResortComparator comparator = new ResortComparator(sort, sortOrder, pc);
		int nextOutputIndex = 1;
		List eqList = pc.getEquipmentMasterList();
		Collections.sort(eqList, comparator);

		for (Iterator eI = eqList.iterator(); eI.hasNext();)
		{
			final Equipment item = (Equipment) eI.next();

			if (item.getOutputIndex() >= 0)
			{
				item.setOutputIndex(nextOutputIndex++);
			}
		}

		selectedModel.updateTree();
		selectedTable.updateUI();
		pc.setDirty(true);
	}

	private void clearAvailableQFilter()
	{
		availableModel.clearQFilter();
		if (saveAvailableViewMode != null)
		{
			viewMode = saveAvailableViewMode.intValue();
			saveAvailableViewMode = null;
		}
		availableModel.resetModel(viewMode, true);
		clearAvailableQFilterButton.setEnabled(false);
		viewComboBox.setEnabled(true);
		forceRefresh();
	}

	private void clearSelectedQFilter()
	{
		selectedModel.clearQFilter();
		if (saveSelectedViewMode != null)
		{
			viewSelectMode = saveSelectedViewMode.intValue();
			saveSelectedViewMode = null;
		}
		selectedModel.resetModel(viewSelectMode, false);
		clearSelectedQFilterButton.setEnabled(false);
		viewSelectComboBox.setEnabled(true);
		forceRefresh();
	}

	private void setAvailableQFilter()
	{
		String aString = textAvailableQFilter.getText();

		if (aString.length() != 0)
		{
			availableModel.setQFilter(aString);
		}

		if (saveAvailableViewMode == null)
		{
			saveAvailableViewMode = new Integer(viewMode);
		}
		viewMode = GuiConstants.INFOINVENTORY_VIEW_NAME;
		availableModel.resetModel(viewMode, true);
		clearAvailableQFilterButton.setEnabled(true);
		viewComboBox.setEnabled(false);
		forceRefresh();
		
	}

	private void setSelectedQFilter()
	{
		String aString = textSelectedQFilter.getText();

		if (aString.length() != 0)
		{
			selectedModel.setQFilter(aString);
		}

		if (saveSelectedViewMode == null)
		{
			saveSelectedViewMode = new Integer(viewSelectMode);
		}
		viewSelectMode = GuiConstants.INFOINVENTORY_VIEW_NAME;
		selectedModel.resetModel(viewMode, false);
		clearSelectedQFilterButton.setEnabled(true);
		viewSelectComboBox.setEnabled(false);
		forceRefresh();
	}

	/**
	 * This method causes the character to sell all of a
	 * given equipment item from inventory at the
	 * rate specified in the rate combo box.
	 * @param selectedEquipment Equipment to sell
	 **/
	private void sellSpecifiedEquipmentRate(Equipment selectedEquipment)
	{
		sellSpecifiedEquipmentRate(selectedEquipment, selectedEquipment.qty());
	}

	/**
	 * This method causes the character to sell a number of the
	 * given equipment item from inventory at the rate specified
	 *
	 * @param selectedEquipment Equipment to sell
	 * @param qty double number of the item to sell
	 **/
	private void sellSpecifiedEquipmentRate(Equipment selectedEquipment, double qty)
	{
		Object defaultValue = cmbSellPercent.getSelectedItem().toString();
		InputInterface ii = InputFactory.getInputInstance();
		Object input = ii.showInputDialog(this, "Enter sell price percentage:", "Sell at Percent",
				MessageType.QUESTION, null, defaultValue);

		if (input != null)
		{
			String iString = input.toString().trim();

			try
			{
				int sellRate = Integer.parseInt(iString);
				sellSpecifiedEquipmentRate(selectedEquipment, qty, sellRate);
			}
			catch (NumberFormatException nfe)
			{
				ShowMessageDelegate.showMessageDialog("You must enter an integer value.", "Error", MessageType.ERROR);
			}
		}
	}

	/**
	 * This method causes the character to sell a number of the
	 * given equipment item from inventory at the rate specified.
	 *
	 * @param selectedEquipment Equipment to sell
	 * @param qty double number of the item to sell
	 * @param sellRate int rate percentage (typically 0 to 100) at which to sell the item
	 **/
	private void sellSpecifiedEquipmentRate(Equipment selectedEquipment, double qty, int sellRate)
	{
		adjustGold(selectedEquipment, removeSpecifiedEquipment(selectedEquipment, qty), 0, sellRate);
	}

	/**
	 * Updates the Available table
	 **/
	private void updateAvailableModel()
	{
		List pathList = availableTable.getExpandedPaths();
		createAvailableModel();
		availableTable.updateUI();
		availableTable.expandPathList(pathList);
	}

	private void updateEqInfo(Equipment selectedEquipment)
	{
		updateTotalValue();

		if (selectedEquipment.hasVFeats())
		{
			// Virtual feat list might change so need
			// to update the list as well as the feat tab
			pc.setVirtualFeatsStable(false);
		}
	}

	/**
	 * Updates the Selected table
	 **/
	private void updateSelectedModel()
	{
		List pathList = selectedTable.getExpandedPaths();
		createSelectedModel();
		selectedTable.updateUI();
		selectedTable.expandPathList(pathList);
	}

	private void updateTotalValue()
	{
		totalValue.setText(BigDecimalHelper.trimZeros(pc.totalValue()) + " " + Globals.getCurrencyDisplay());
	}

	private void viewComboBoxActionPerformed()
	{
		final int index = viewComboBox.getSelectedIndex();

		if (index != viewMode)
		{
			viewMode = index;
			SettingsHandler.setGearTab_AvailableListMode(viewMode);
			updateAvailableModel();
		}
	}

	private void viewSelectComboBoxActionPerformed()
	{
		final int index = viewSelectComboBox.getSelectedIndex();

		if (index != viewSelectMode)
		{
			viewSelectMode = index;
			SettingsHandler.setGearTab_SelectedListMode(viewSelectMode);
			updateSelectedModel();
		}
	}

	/**
	 * This class is an ActionListener used to perform actions such as
	 * add/remove of equipment selected in the equipment tree tables.
	 * This class is an abstract base class for concrete implementations
	 * and provides handling of item quantities.
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private static abstract class GearActionListener implements ActionListener
	{
		int qty = 0;

		private GearActionListener(int aQty)
		{
			qty = aQty;
		}

		protected abstract void updateEquipment(Equipment eq, int argQty);
	}

	/**
	 * This class is an ActionListener used to perform actions such as
	 * add/remove of equipment selected in the available equipment table.
	 * This class is an abstract base class for concrete implementations
	 * and provides determination of which item is currently selected.
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private abstract class AvailableGearActionListener extends GearActionListener
	{
		private AvailableGearActionListener(int aQty)
		{
			super(aQty);
		}

		public void actionPerformed(ActionEvent evt)
		{
			Object pe = getCurrentAvailableTableItem();

			if ((pe != null) && (pe instanceof Equipment))
			{
				updateEquipment((Equipment) pe, qty);
			}
		}
	}

	/**
	 * This class is an ActionListener used to perform actions such as
	 * add/remove of equipment selected in the selected equipment table.
	 * This class is an abstract base class for concrete implementations
	 * and provides determination of which item is currently selected.
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private abstract class SelectedGearActionListener extends GearActionListener
	{
		private SelectedGearActionListener(int aQty)
		{
			super(aQty);
		}

		public void actionPerformed(ActionEvent evt)
		{
			Object pe = getCurrentSelectedTableItem();

			if ((pe != null) && (pe instanceof Equipment))
			{
				updateEquipment((Equipment) pe, qty);
			}
		}
	}

	/**
	 * This class is an ActionListener used to perform a purchase of the
	 * equipment item currently selected in the available table at the
	 * rate selected in the buy rate combo box.
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private class BuyGearActionListener extends AvailableGearActionListener
	{
		private BuyGearActionListener(int qty)
		{
			super(qty);
		}

		protected void updateEquipment(Equipment eq, int argQty)
		{
			buySpecifiedEquipment(eq, argQty);
		}
	}

	/**
	 * This class is an ActionListener used to perform a purchase of the
	 * equipment item currently selected in the available table at the
	 * rate selected at the time the action is performed.
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private class BuyRateGearActionListener extends AvailableGearActionListener
	{
		private BuyRateGearActionListener(int qty)
		{
			super(qty);
		}

		protected void updateEquipment(Equipment eq, int argQty)
		{
			buySpecifiedEquipmentRate(eq, argQty);
		}
	}

	/**
	 * These are the right click popup menus
	 **/
	private class GearPopupMenu extends JPopupMenu
	{
		static final long serialVersionUID = -2320970658737297916L;

		private GearPopupMenu(JTreeTable treeTable)
		{
			if (treeTable == availableTable)
			{
				GearPopupMenu.this.add(createBuyMenuItem("Buy  1", 1, "shortcut EQUALS"));

				JMenu buyMenu = Utility.createMenu("Buy # ...", (char) 0, "Buy # ...", null, true);

				buyMenu.add(createBuyMenuItem("Buy  2", 2, null));
				buyMenu.add(createBuyMenuItem("Buy  5", 5, null));
				buyMenu.add(createBuyMenuItem("Buy 10", 10, null));
				buyMenu.add(createBuyMenuItem("Buy 15", 15, null));
				buyMenu.add(createBuyMenuItem("Buy 20", 20, null));
				buyMenu.add(createBuyMenuItem("Buy 50", 50, null));
				GearPopupMenu.this.add(buyMenu);
				GearPopupMenu.this.add(createBuyMenuItem("Buy  n", -1, "alt N"));
				this.addSeparator();
				GearPopupMenu.this.add(createBuyRateMenuItem("Buy  1 at...", 1, null));

				JMenu buyAtMenu = Utility.createMenu("Buy # at ...", (char) 0, "Buy # at ...", null, true);
				buyAtMenu.add(createBuyRateMenuItem("Buy  2 at...", 2, null));
				buyAtMenu.add(createBuyRateMenuItem("Buy  5 at...", 5, null));
				buyAtMenu.add(createBuyRateMenuItem("Buy 10 at...", 10, null));
				buyAtMenu.add(createBuyRateMenuItem("Buy 15 at...", 15, null));
				buyAtMenu.add(createBuyRateMenuItem("Buy 20 at...", 20, null));
				buyAtMenu.add(createBuyRateMenuItem("Buy 50 at...", 50, null));
				GearPopupMenu.this.add(buyAtMenu);
				GearPopupMenu.this.add(createBuyRateMenuItem("Buy  n at...", -1, "alt N"));
				this.addSeparator();

				GearPopupMenu.this.add(Utility.createMenuItem("Create custom item",
						new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							customizeButtonClick();
						}
					}, "newCustomItem", (char) 0, "alt C", "Create new customized item", null, true));
				GearPopupMenu.this.add(Utility.createMenuItem("Delete custom item",
						new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							deleteCustomButtonClick();
						}
					}, "deleteItem", (char) 0, "DELETE", "Delete custom item", null, true));

/*                GearPopupMenu.this.add(CoreUtility.createMenuItem("Create custom item from scratch",
   new ActionListener()
   {
	   public void actionPerformed(ActionEvent e)
	   {
		   createGenericItemButtonClick();
	   }
   }
   , "createGenericItem", (char)0, "GENERIC", "Create custom item from scratch", null, true));
 */
			}

			else // selectedTable
			{
				GearPopupMenu.this.add(createRemoveMenuItem("Remove  1", 1, "shortcut MINUS"));

				JMenu remMenu = Utility.createMenu("Remove # ...", (char) 0, "Remove # ...", null, true);
				remMenu.add(createRemoveMenuItem("Remove  2", 2, null));
				remMenu.add(createRemoveMenuItem("Remove  5", 5, null));
				remMenu.add(createRemoveMenuItem("Remove 10", 10, null));
				remMenu.add(createRemoveMenuItem("Remove 15", 15, null));
				remMenu.add(createRemoveMenuItem("Remove 20", 20, null));
				remMenu.add(createRemoveMenuItem("Remove 50", 50, null));
				remMenu.add(createRemoveMenuItem("Remove  n", -1, null));
				GearPopupMenu.this.add(remMenu);
				GearPopupMenu.this.add(createRemoveMenuItem("Remove All", -5, null));
				this.addSeparator();

				GearPopupMenu.this.add(createSellMenuItem("Sell  1", 1, null));

				JMenu sellMenu = Utility.createMenu("Sell # ...", (char) 0, "Sell # ...", null, true);
				sellMenu.add(createSellMenuItem("Sell  2", 2, null));
				sellMenu.add(createSellMenuItem("Sell  5", 5, null));
				sellMenu.add(createSellMenuItem("Sell 10", 10, null));
				sellMenu.add(createSellMenuItem("Sell 15", 15, null));
				sellMenu.add(createSellMenuItem("Sell 20", 20, null));
				sellMenu.add(createSellMenuItem("Sell 50", 50, null));
				sellMenu.add(createSellMenuItem("Sell  n", -1, null));
				GearPopupMenu.this.add(sellMenu);
				GearPopupMenu.this.add(createSellMenuItem("Sell  All", -5, null));
				this.addSeparator();

				GearPopupMenu.this.add(createSellRateMenuItem("Sell 1 at...", 1, null));
				GearPopupMenu.this.add(createSellRateMenuItem("Sell n at...", -1, null));
				GearPopupMenu.this.add(createSellRateMenuItem("Sell All at...", -5, null));
				this.addSeparator();

				GearPopupMenu.this.add(Utility.createMenuItem("Modify Charges",
						new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							editChargesButtonClicked();
						}
					}, "editCharges", (char) 0, "shortcut ?", "Edit charges", null, true));
				this.addSeparator();


				GearPopupMenu.this.add(pcMoveMenu);
				GearPopupMenu.this.add(pcCopyMenu);
				this.addSeparator();

				JMenu resortMenu = Utility.createMenu("Output Order", (char) 0, "Output Order", null, true);

				GearPopupMenu.this.add(resortMenu);

				resortMenu.add(Utility.createMenuItem("By name (ascending)",
						new ResortActionListener(ResortComparator.RESORT_NAME, ResortComparator.RESORT_ASCENDING),
						"sortOutput", (char) 0, null, "Sort equipment list by name in ascending alphabetical order",
						null, true));
				resortMenu.add(Utility.createMenuItem("By name (descending)",
						new ResortActionListener(ResortComparator.RESORT_NAME, ResortComparator.RESORT_DESCENDING),
						"sortOutput", (char) 0, null, "Sort equipment list by name in descending alphabetical order",
						null, true));
				resortMenu.add(Utility.createMenuItem("By weight (ascending)",
						new ResortActionListener(ResortComparator.RESORT_WEIGHT, ResortComparator.RESORT_ASCENDING),
						"sortOutput", (char) 0, null, "Sort equipment list by weight in ascending order", null, true));
				resortMenu.add(Utility.createMenuItem("By weight (descending)",
						new ResortActionListener(ResortComparator.RESORT_WEIGHT, ResortComparator.RESORT_DESCENDING),
						"sortOutput", (char) 0, null, "Sort equipment list by weight in descending order", null, true));
			}
		}

		private JMenuItem createBuyMenuItem(String label, int qty, String accelerator)
		{
			return Utility.createMenuItem(label, new BuyGearActionListener(qty), "Buy" + qty, (char) 0, accelerator,
				"Buy " + ((qty < 0) ? "n" : Integer.toString(qty)) + " at the current rate", "Add16.gif", true);
		}

		private JMenuItem createBuyRateMenuItem(String label, int qty, String accelerator)
		{
			return Utility.createMenuItem(label, new BuyRateGearActionListener(qty), "Buy" + qty, (char) 0,
				accelerator, "Buy " + ((qty < 0) ? "n" : Integer.toString(qty)) + " at a specified rate", "Add16.gif",
				true);
		}

		private JMenuItem createRemoveMenuItem(String label, int qty, String accelerator)
		{
			return Utility.createMenuItem(label, new RemoveGearActionListener(qty), "Remove" + qty, (char) 0,
				accelerator, "Remove " + ((qty < 0) ? "n" : Integer.toString(qty)) + " from your inventory",
				"Remove16.gif", true);
		}

		private JMenuItem createSellMenuItem(String label, int qty, String accelerator)
		{
			return Utility.createMenuItem(label, new SellGearActionListener(qty), "Sell" + qty, (char) 0, accelerator,
				"Sell " + ((qty < 0) ? "n" : Integer.toString(qty)) + " from your inventory", null, true);
		}

		private JMenuItem createSellRateMenuItem(String label, int qty, String accelerator)
		{
			return Utility.createMenuItem(label, new SellRateGearActionListener(qty), "Sell" + qty, (char) 0,
				accelerator, "Sell " + ((qty < 0) ? "n" : Integer.toString(qty)) + " from your inventory at a rate",
				null, true);
		}
	}

	private class MoveItemListener implements ActionListener
	{
		int moveType; // 0=move, 1=copy
		int pcIndex;

		/**
		 * Constructor
		 * @param index
		 * @param typeOfMove
		 */
		public MoveItemListener(int index, int typeOfMove)
		{
			pcIndex = index;
			moveType = typeOfMove;
		}

		public void actionPerformed(ActionEvent e)
		{
			PlayerCharacter playerCharacter = (PlayerCharacter) Globals.getPCList().get(pcIndex);
			Equipment eq = (Equipment) getCurrentSelectedTableItem();

			if (eq == null)
			{
				return;
			}

			if (moveType == 0) // move
			{
				playerCharacter.addEquipment((Equipment) eq.clone());
				playerCharacter.setDirty(true);
				pc.removeEquipment(eq);
				pc.setDirty(true);
			}
			else
			{
				playerCharacter.addEquipment((Equipment) eq.clone());
			}

			playerCharacter.setDirty(true);
			updateSelectedModel();
		}
	}

	/**
	 * This class is an ActionListener used to perform a removal of the
	 * equipment item currently selected in the selected table.  Note this
	 * would be equivalent to such an action as drinking a potion, where no
	 * currency is received for the item.
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private class RemoveGearActionListener extends SelectedGearActionListener
	{
		RemoveGearActionListener(int qty)
		{
			super(qty);
		}

		protected void updateEquipment(Equipment eq, int argQty)
		{
			if (argQty < -1)
			{
				removeSpecifiedEquipment(eq, eq.qty());
			}
			else
			{
				removeSpecifiedEquipment(eq, argQty);
			}
		}
	}

	private class ResortActionListener implements ActionListener
	{
		boolean sortOrder;
		int sort;

		/**
		 * Constructor
		 * @param i
		 * @param aBool
		 */
		public ResortActionListener(int i, boolean aBool)
		{
			sort = i;
			sortOrder = aBool;
		}

		public void actionPerformed(ActionEvent e)
		{
			resortSelected(sort, sortOrder);
		}
	}

	private static class ResortComparator implements Comparator
	{
		/** The name of the re-sort */
		public static final int RESORT_NAME = 0;
		/** The weight of the re-sort */
		public static final int RESORT_WEIGHT = 1;
		/** The re-sort is ascending by default */
		public static final boolean RESORT_ASCENDING = true;
		/** The re-sort is not descending by default */
		public static final boolean RESORT_DESCENDING = false;
		/** The sort order is set to RESORT_ASCENDING by default */
		private boolean sortOrder = RESORT_ASCENDING;
		private int sort = RESORT_NAME;
		private PlayerCharacter pc;

		/**
		 * Constructor
		 * @param sort
		 * @param sortOrder
		 * @param pc
		 */
		public ResortComparator(int sort, boolean sortOrder, PlayerCharacter pc)
		{
			this.sort = sort;
			this.sortOrder = sortOrder;
			this.pc = pc;
		}

		// Comparator will be specific to Equipment objects
		public int compare(Object obj1, Object obj2)
		{
			Equipment e1;
			Equipment e2;

			if (sortOrder == RESORT_ASCENDING)
			{
				e1 = (Equipment) obj1;
				e2 = (Equipment) obj2;
			}
			else
			{
				e1 = (Equipment) obj2;
				e2 = (Equipment) obj1;
			}

			switch (sort)
			{
				case RESORT_WEIGHT:
					return e1.getWeight(pc).compareTo(e2.getWeight(pc));

				case RESORT_NAME:default:
					return e1.getName().compareToIgnoreCase(e2.getName());
			}
		}
	}

	private class GearPopupListener extends MouseAdapter
	{
		private GearPopupMenu menu;
		private JTree tree;

		private GearPopupListener(JTreeTable treeTable, GearPopupMenu aMenu)
		{
			tree = treeTable.getTree();
			menu = aMenu;

			KeyListener myKeyListener = new KeyListener()
				{
					public void keyTyped(KeyEvent e)
					{
						dispatchEvent(e);
					}

					//
					// Walk through the list of accelerators to see if the user has
					// pressed a sequence used by the popup.
					// This would not otherwise happen unless the popup was showing
					//
					public void keyPressed(KeyEvent e)
					{
						final int keyCode = e.getKeyCode();

						if (keyCode != KeyEvent.VK_UNDEFINED)
						{
							final KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);

							for (int i = 0; i < menu.getComponentCount(); i++)
							{
								final Component menuComponent = menu.getComponent(i);

								if (menuComponent instanceof JMenuItem)
								{
									KeyStroke ks = ((JMenuItem) menuComponent).getAccelerator();

									if ((ks != null) && keyStroke.equals(ks))
									{
										selPath = tree.getSelectionPath();
										((JMenuItem) menuComponent).doClick(2);

										return;
									}
								}
							}
						}

						dispatchEvent(e);
					}

					public void keyReleased(KeyEvent e)
					{
						dispatchEvent(e);
					}
				};

			treeTable.addKeyListener(myKeyListener);
		}

		public void mousePressed(MouseEvent evt)
		{
			maybeShowPopup(evt);
		}

		public void mouseReleased(MouseEvent evt)
		{
			maybeShowPopup(evt);
		}

		private void maybeShowPopup(MouseEvent evt)
		{
			if (evt.isPopupTrigger())
			{
				selPath = tree.getClosestPathForLocation(evt.getX(), evt.getY());

				if (selPath == null)
				{
					return;
				}

				if (tree.isSelectionEmpty())
				{
					tree.setSelectionPath(selPath);
					// Workaround for swing bug - initial row wasn't being selected.
					tree.getSelectionModel().resetRowSelection();
					menu.show(evt.getComponent(), evt.getX(), evt.getY());
				}
				else if (!tree.isPathSelected(selPath))
				{
					tree.setSelectionPath(selPath);
					menu.show(evt.getComponent(), evt.getX(), evt.getY());
				}
				else
				{
					tree.addSelectionPath(selPath);
					menu.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}
		}
	}

	/**
	 * OutputOrderEditor is a JCombobox based table cell editor.
	 * It allows the user to either enter their own output order index
	 * or to select from hidden, first or last. If first or last are
	 * selected, then special values are returned to the setValueAt
	 * method, which are actioned by that method.
	 **/
	private static final class OutputOrderEditor extends JComboBoxEx implements TableCellEditor
	{
		private final transient List d_listeners = new ArrayList();
		private transient int d_originalValue = 0;

		private OutputOrderEditor(String[] choices)
		{
			super(choices);

			setEditable(true);

			addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent ae)
					{
						stopCellEditing();
					}
				});
		}

		public boolean isCellEditable(EventObject eventObject)
		{
			return true;
		}

		public Object getCellEditorValue()
		{
			switch (this.getSelectedIndex())
			{
				case 0: // First
					return new Integer(0);

				case 1: // Last
					return new Integer(1000);

				case 2: // Hidden
					return new Integer(-1);

				default: // A number

					return new Integer((String) getSelectedItem());
			}
		}

		public Component getTableCellEditorComponent(JTable jTable, Object value, boolean isSelected, int row,
			int column)
		{
			if (value == null)
			{
				return this;
			}

			d_originalValue = this.getSelectedIndex();

			if (value instanceof Integer)
			{
				int i = ((Integer) value).intValue();

				if (i == -1)
				{
					setSelectedItem("Hidden");
				}
				else
				{
					setSelectedItem(String.valueOf(i));
				}
			}
			else
			{
				setSelectedItem("Hidden");
			}

			jTable.setRowSelectionInterval(row, row);
			jTable.setColumnSelectionInterval(column, column);

			return this;
		}

		public void addCellEditorListener(CellEditorListener cellEditorListener)
		{
			d_listeners.add(cellEditorListener);
		}

		public void cancelCellEditing()
		{
			fireEditingCanceled();
		}

		public void removeCellEditorListener(CellEditorListener cellEditorListener)
		{
			d_listeners.remove(cellEditorListener);
		}

		public boolean shouldSelectCell(EventObject eventObject)
		{
			return true;
		}

		public boolean stopCellEditing()
		{
			fireEditingStopped();

			return true;
		}

		private void fireEditingCanceled()
		{
			setSelectedIndex(d_originalValue);

			ChangeEvent ce = new ChangeEvent(this);

			for (int i = d_listeners.size() - 1; i >= 0; --i)
			{
				((CellEditorListener) d_listeners.get(i)).editingCanceled(ce);
			}
		}

		private void fireEditingStopped()
		{
			ChangeEvent ce = new ChangeEvent(this);

			for (int i = d_listeners.size() - 1; i >= 0; --i)
			{
				((CellEditorListener) d_listeners.get(i)).editingStopped(ce);
			}
		}
	}
	 //End OutputOrderEditor classes

	/**
	 * OutputOrderRenderer is a small extension of the standard JLabel based
	 * table cell renderer that allows it to interpret a few special values
	 * 
	 * -1 shows as Hidden, and 0 is shown as blank. Any other value is
	 * displayed as is.
	 */
	private static final class OutputOrderRenderer extends DefaultTableCellRenderer
	{
		private OutputOrderRenderer()
		{
			super();
			setHorizontalAlignment(SwingConstants.CENTER);
		}

		public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected,
			boolean hasFocus, int row, int column)
		{
			JLabel comp = (JLabel) super.getTableCellRendererComponent(jTable, value, isSelected, hasFocus, row, column);

			if (value instanceof Integer)
			{
				int i = ((Integer) value).intValue();

				if (i == -1)
				{
					comp.setText("Hidden");
				}
				else if (i == 0)
				{
					comp.setText("");
				}
				else
				{
					comp.setText(String.valueOf(i));
				}
			}

			return comp;
		}
	}

	/**
	 * In the TreeTableModel there is a single <code>root</code>
	 * object.  This root object has a null <code>parent</code>.
	 * All other objects have a parent which points to a non-null object.
	 * parent objects contain a list of <code>children</code>, which are
	 * all the objects that point to it as their parent.
	 * objects (or <code>nodes</code>) which have 0 children
	 * are leafs (the end of that linked list).
	 * nodes which have at least 1 child are not leafs. Leafs are like files
	 * and non-leafs are like directories.
	 **/
	private final class EquipmentModel extends AbstractTreeTableModel {
		private int currentMode = GuiConstants.INFOINVENTORY_VIEW_TYPE_SUBTYPE_NAME;

		// Types of the columns.
		private int modelType = 0; // availableModel=0,selectedModel=1

		/**
		 * Creates a EquipmentModel
		 * @param mode
		 * @param available
		 **/
		private EquipmentModel(int mode, boolean available) {
			super(null);

			if (!available) {
				modelType = 1;
			}

			resetModel(mode, available);
		}

		public boolean isCellEditable(Object node, int column) {
			return ((column == COL_NAME)
			|| ((modelType == 1) && (((PObjectNode) node).getItem() instanceof Equipment)
			&& ((column == COL_QTY_SRC) || (column == COL_INDEX))));
		}

		/**
		 * Returns Class for the column.
		 * @param column
		 * @return Class
		 */
		public Class getColumnClass(int column) {
			switch (column) {
				case COL_NAME:
					return TreeTableModel.class;

				case COL_COST:
					return BigDecimal.class;

				case COL_QTY_SRC:
					if (modelType == 0) {
						return String.class;
					}

					return Float.class;

				case COL_INDEX:
					return Integer.class;

				default:
					Logging.errorPrint("In InfoGear.EquipmentModel.getColumnClass the column " + column
						+ " is not supported.");

					break;
			}

			return String.class;
		}

		/* The JTreeTableNode interface. */

		/**
		 * Returns int number of columns.
		 * @return column count
		 */
		public int getColumnCount() {
			if (modelType == 0) {
				return NUM_COL_AVAILABLE;
			}

			return NUM_COL_SELECTED;
		}

		/**
		 * Returns String name of a column.
		 * @param column
		 * @return column name
		 */
		public String getColumnName(int column) {
			switch (column) {
				case COL_NAME:
					return "Item";

				case COL_COST:
					return "Cost";

				case COL_QTY_SRC:
					if (modelType == 0) {
						return "Source";
					}
					return "Qty";

				case COL_INDEX:
					return "Order";

				default:
					Logging.errorPrint("In InfoGear.EquipmentModel.getColumnName the column " + column + " is not supported.");
					return "";
			}
		}

		public Object getRoot() {
			return (PObjectNode) super.getRoot();
		}

		public void setValueAt(Object value, Object node, int column) {
			if (pc == null) {
				return;
			}

			if (modelType != 1) {
				return; // can only set values for selectedTableModel
			}

			Object obj = ((PObjectNode) node).getItem();

			if (!(obj instanceof Equipment)) {
				return; // can only use rows with Equipment in them
			}

			Equipment selectedEquipment = (Equipment)obj;

			if (getBaseEquipment(selectedEquipment) == null) {
				return;
			}

			switch (column) {
				case COL_QTY_SRC:
					setColumnQtySrc(selectedEquipment, value);
					break;

				case COL_INDEX:
					setColumnIndex(selectedEquipment, value);
					break;

				default:
					Logging.errorPrint("In InfoGear.EquipmentModel.setValueAt the column " + column + " is not supported.");
					break;
			}
		}

		private void setColumnQtySrc(Equipment eq, Object value) {
			double qtyToAdd = ((Float) value).floatValue() - eq.qty();

			if (qtyToAdd > 0.0) {
				buySpecifiedEquipment(eq, qtyToAdd);
			}
			else if (qtyToAdd < 0.0) {
				sellSpecifiedEquipment(eq, -qtyToAdd);
			}
		}

		private void setColumnIndex(Equipment eq, Object value) {
			int outputIndex = ((Integer) value).intValue();
			int workingIndex = 1;

			if (outputIndex == 1000) { // Last
				// Set it to one higher that the highest output index so far
				outputIndex = getHighestOutputIndex() + 1;
			}
			else if (outputIndex == 0) { // First
				// Set it to 1 and shuffle everyone up in order
				outputIndex = 1;
				workingIndex = 2;
			}

			eq.setOutputIndex(outputIndex);

			for (Iterator i = pc.getEquipmentMasterListInOutputOrder().iterator(); i.hasNext();) {
				final Equipment item = (Equipment) i.next();

				if (workingIndex == outputIndex) {
					workingIndex++;
				}

				if ((item.getOutputIndex() > -1) && (item != eq)) {
					item.setOutputIndex(workingIndex++);
				}
			}

			selectedModel.updateTree();
			selectedTable.updateUI();
			pc.setDirty(true);
		}

		/**
		 * Returns Object value of the column.
		 * @param node
		 * @param column
		 * @return value
		 */
		public Object getValueAt(Object node, int column) {
			final PObjectNode fn = (PObjectNode) node;

			switch (column) {
				case COL_NAME: // Name
					return getColumnName(fn);

				case COL_COST: // Cost
					return getColumnCost(fn);

				case COL_QTY_SRC: // Source or Qty
					return getColumnQtySrc(fn);

				case COL_INDEX: // Output index
					return getColumnIndex(fn);

				case -1:
					return getColumnEquipment(fn);

				default:
					Logging.errorPrint("In InfoGear.EquipmentModel.getValueAt the column " + column + " is not supported.");
				  return null;
			}
		}

		private Object getColumnEquipment(PObjectNode fn) {
			if (fn != null) {
				return fn.getItem();
			}
			Logging.errorPrint("Somehow we have no active node when doing getValueAt in InfoGear.");
			return null;
		}

		private Object getColumnName(PObjectNode fn) {
			if (fn != null) {
				return fn.toString();
			}
			Logging.errorPrint("Somehow we have no active node when doing getValueAt in InfoGear.");
			return "";
		}

		private Object getColumnCost(PObjectNode fn) {
			if((fn != null) && (fn.getItem() instanceof Equipment)) {
				Equipment aEq = (Equipment) fn.getItem();
				return BigDecimalHelper.trimBigDecimal(aEq.getCost(pc));
			}
			return null;
		}

		private Object getColumnQtySrc(PObjectNode fn) {
			if ((fn != null) && (modelType == 0)) {
				StringBuffer b = new StringBuffer();
				b.append("   ");
				b.append(fn.getSource());

				return b.toString();
			}

			if((fn != null) && (fn.getItem() instanceof Equipment)) {
				Equipment aEq = (Equipment) fn.getItem();
				return new Float(aEq.qty());
			}
			return null;
		}

		private Object getColumnIndex(PObjectNode fn) {
			int outputIndex = 0;
			if((fn != null) && (fn.getItem() instanceof Equipment)) {
				Equipment aEq = (Equipment) fn.getItem();
				outputIndex = aEq.getOutputIndex();
			}

			return new Integer(outputIndex);
		}

		private Equipment getBaseEquipment(Equipment selectedEquipment) {
			String keyName = selectedEquipment.getKeyName();
			Equipment baseEquipment = EquipmentList.getEquipmentNamed(keyName);

			if (baseEquipment == null) {
				baseEquipment = pc.getEquipmentNamed(keyName);
			}

			return baseEquipment;
		}

		/** 
		 * "There can be only one" There must be a root object, though it can be hidden
		 * to make it's existence basically a convenient way to keep track of the objects
		 * 
		 * @param aNode
		 */
		private void setRoot(PObjectNode aNode) {
			super.setRoot(aNode);
		}

		private void setValueForItemInNodes(PObjectNode p, Equipment e, double f, int column) {
			if (p == null) {
				p = (PObjectNode) super.getRoot();
			}

			Object obj = p.getItem();

			// if no children, remove it and update parent
			if ((p.getChildCount() == 0) && (obj != null) && (obj instanceof Equipment) && obj.equals(e)) {
				final Equipment pe = (Equipment) obj;

				switch (column) {
					case COL_QTY_SRC:
						pe.setQty(new Float(f));

						if (pe.getCarried().floatValue() > f) {
							pe.setNumberCarried(new Float(f));
						}

						break;

					default:
						Logging.errorPrint("In InfoGear.EquipmentModel.setValueForItemInNodes the column " + column
							+ " is not supported.");

						break;
				}
			}
			else {
				for (int i = 0; i < p.getChildCount(); i++) {
					setValueForItemInNodes(p.getChild(i), e, f, column);
				}
			}
		}

		private void addItemToModel(Equipment eq, boolean fireEvent) {
			PObjectNode rootAsPObjectNode = (PObjectNode) super.getRoot();

			if (eq == null || !shouldDisplayThis(eq)) {
				return;
			}

			switch (currentMode) {
				case GuiConstants.INFOINVENTORY_VIEW_TYPE_SUBTYPE_NAME: // Type/SubType/Name
					addChildTypeSubtypeName(eq, rootAsPObjectNode, fireEvent);
					break;

				case GuiConstants.INFOINVENTORY_VIEW_TYPE_NAME: // Type/Name
					addChildTypeName(eq, rootAsPObjectNode, fireEvent);
					break;

				case GuiConstants.INFOINVENTORY_VIEW_NAME: // Name
					addChildName(eq, rootAsPObjectNode, fireEvent);
					break;

				case GuiConstants.INFOINVENTORY_VIEW_ALL_TYPES: // All Types... every unique TYPE is listed
					addChildAllTypes(eq, rootAsPObjectNode, fireEvent);
					break;

				case GuiConstants.INFOINVENTORY_VIEW_SOURCE_NAME: // Source/Name
					addChildSourceName(eq, rootAsPObjectNode, fireEvent);
					break;

				default:
					Logging.errorPrint("In InfoGear.EquipmentModel.addItemToModel (second switch) the mode " + currentMode + " is not supported.");
					break;
			}
		}

		private void addChildTypeSubtypeName(Equipment eq, PObjectNode rootAsPObjectNode, boolean fireEvent) {
			if (fireEvent) {
				//Add custom node if it does not exist
				if (eq.isType(Constants.s_CUSTOM)) {
					addChild(Constants.s_CUSTOM, typeSubtypeRoot, true);
				}

				//Add item's type to the root
				String type = eq.typeIndex(0);
				addChild(type, typeSubtypeRoot, true);

				// Now add any missing subtypes to type/subtype/name tree
				PObjectNode typeSubtypeRootAsPObjectNode = (PObjectNode) typeSubtypeRoot;
				for (Iterator e = eq.typeList().iterator(); e.hasNext(); ) {
					type = (String) e.next();

					for (int i = 0; i < typeSubtypeRootAsPObjectNode.getChildCount(); i++) {
						final String treeType = typeSubtypeRootAsPObjectNode.getChild(i).toString();
						if ((typeSubtypeRootAsPObjectNode.getChild(i).getItem() instanceof PObject)
								|| !eq.isType(treeType)
								|| type.equals(treeType)) {
							continue;
						}
						addChild(type, typeSubtypeRootAsPObjectNode.getChild(i), true);
					}
				}
			}

			//Add Equipment
			for (int i = 0; i < rootAsPObjectNode.getChildCount(); i++) {
				if (eq.isType(rootAsPObjectNode.getChild(i).toString())) {
					// Items with only 1 type will not show up unless we do this
					List d;

					if (eq.typeList().size() == 1) {
						d = new ArrayList(1);
						d.add(rootAsPObjectNode.getChild(i));
					}
					else {
						d = rootAsPObjectNode.getChild(i).getChildren();
					}

					for (int k = 0; (d != null) && (k < d.size()); k++) {
						// Don't add children to items (those with only 1 type)
						if (!(((PObjectNode) d.get(k)).getItem() instanceof PObject)) {
							if (eq.isType(((PObjectNode) d.get(k)).toString())) {
								addChild(eq, d.get(k), fireEvent);
							}
						}
					}
				}
			}
		}

		private void addChildTypeName(Equipment eq, PObjectNode rootAsPObjectNode, boolean fireEvent) {
			if(fireEvent) {
				//Add custom node if it does not exist
				if (eq.isType(Constants.s_CUSTOM)) {
					addChild(Constants.s_CUSTOM, typeRoot, true);
				}

				//Add Type
				String type = eq.typeIndex(0);
				addChild(type, typeRoot, true);
			}

			//Add Equipment
			int length = rootAsPObjectNode.getChildCount(); // seperated out for performance reasons
			for (int i = 0; i < length; i++) {
				if (eq.isType(rootAsPObjectNode.getChild(i).toString())) {
					addChild(eq, rootAsPObjectNode.getChild(i), fireEvent);
				}
			}
		}

		private void addChildName(Equipment eq, PObjectNode rootAsPObjectNode, boolean fireEvent) {
			//Add Equipment
			addChild(eq, rootAsPObjectNode, fireEvent);
		}

		private void addChildAllTypes(Equipment eq, PObjectNode rootAsPObjectNode, boolean fireEvent) {
			if(fireEvent) {
				// Add Types
				for (Iterator e = eq.typeList().iterator(); e.hasNext(); ) {
					String type = (String) e.next();
					addChild(type, allTypeRoot, true);
				}
			}

			//Add Equipment
			for (int i = 0; i < rootAsPObjectNode.getChildCount(); i++) {
				if (eq.isType(rootAsPObjectNode.getChild(i).toString())) {
					addChild(eq, rootAsPObjectNode.getChild(i), fireEvent);
				}
			}
		}

		private void addChildSourceName(Equipment eq, PObjectNode rootAsPObjectNode, boolean fireEvent) {
			if(fireEvent) {
				//Add custom node if it does not exist
				if (eq.isType(Constants.s_CUSTOM)) {
					addChild(Constants.s_CUSTOMSOURCE, sourceRoot, true);
				}

				//Add Type
				String source = eq.getSourceWithKey("LONG");
				addChild(source, sourceRoot, true);
			}

			//Add Equipment
			int length = rootAsPObjectNode.getChildCount(); // seperated out for performance reasons
			for (int i = 0; i < length; i++) {
				if (eq.getSourceWithKey("LONG").equals(rootAsPObjectNode.getChild(i).toString())) {
					addChild(eq, rootAsPObjectNode.getChild(i), fireEvent);
				}
			}
		}

		private boolean addChild(Object aChild, Object aParent, boolean sort) {
			PObjectNode aFN = new PObjectNode();
			aFN.setItem(aChild);
			aFN.setParent((PObjectNode) aParent);

			if (aChild instanceof Equipment) {
				Equipment eq = (Equipment) aChild;
				PrereqHandler.passesAll( eq.getPreReqList(), pc, eq);
			}

			return ((PObjectNode) aParent).addChild(aFN, sort);
		}

		private void removeItemFromNodes(PObjectNode p, Object e) {
			if (p == null) {
				p = (PObjectNode) super.getRoot();
			}
			p.removeItemFromNodes(e);
		}

		/**
		 * This assumes the EquipmentModel exists but needs to be repopulated
		 * @param mode
		 * @param available
		 */
		private void resetModel(int mode, boolean available) {
			Iterator fI;

			//TODO (DJ) Equipment fix, make this more efficient
			if (available) {
				fI = EquipmentList.getEquipmentList().iterator();
			}
			else {
				fI = pc.getEquipmentMasterList().iterator();
			}

			currentMode = mode;
			String qFilter = this.getQFilter();

			switch (mode) {
				case GuiConstants.INFOINVENTORY_VIEW_TYPE_SUBTYPE_NAME: // Type/SubType/Name
					setRoot((PObjectNode) ((PObjectNode) typeSubtypeRoot).clone());
					break;

				case GuiConstants.INFOINVENTORY_VIEW_TYPE_NAME: // Type/Name
					setRoot((PObjectNode) ((PObjectNode) typeRoot).clone());
					break;

				case GuiConstants.INFOINVENTORY_VIEW_NAME: // Name
					setRoot(new PObjectNode()); // just need a blank one
					break;

				case GuiConstants.INFOINVENTORY_VIEW_ALL_TYPES: // All Types... every unique TYPE is listed
					setRoot((PObjectNode) ((PObjectNode) allTypeRoot).clone());
					break;

				case GuiConstants.INFOINVENTORY_VIEW_SOURCE_NAME: // Type/Name
					setRoot((PObjectNode) ((PObjectNode) sourceRoot).clone());
					break;

				default:
					Logging.errorPrint("In InfoGear.EquipmentModel.resetModel the mode " + mode + " is not supported.");
					break;
			}

			while (fI.hasNext()) {
				final Equipment aEq = (Equipment) fI.next();
				if (qFilter == null || 
						( aEq.getName().toLowerCase().indexOf(qFilter) >= 0 || 
						  aEq.getType().toLowerCase().indexOf(qFilter) >= 0 ))
				{
					addItemToModel(aEq, false);
				}
			}

			PObjectNode rootAsmyPONode = (PObjectNode) super.getRoot();

			if (rootAsmyPONode.getChildCount() > 0) {
				fireTreeNodesChanged(super.getRoot(), new TreePath(super.getRoot()));
			}
		}

		/**
		 * return a boolean to indicate if the item should be included in the list.
		 * Only Weapon, Armor and Shield type items should be checked for proficiency.
		 *
		 * update for new filtering
		 * author: Thomas Behr 09-02-02
		 * @param equip
		 * @return true if should be displayed
		 */
		private boolean shouldDisplayThis(Equipment equip) {
			if (modelType == 0) {
				return accept(pc, equip);
			}
			return true;
		}
	}

	private static final class QuantityEditor extends JTextField implements TableCellEditor
	{
		private final transient List d_listeners = new ArrayList();
		private transient String d_originalValue = "";

		private QuantityEditor()
		{
			super();
			this.setAlignmentX(Component.RIGHT_ALIGNMENT);
		}

		public boolean isCellEditable(EventObject eventObject)
		{
			return true;
		}

		public Object getCellEditorValue()
		{
			try
			{
				return new Float(getText());
			}
			catch (NumberFormatException nfe)
			{
				return new Float(d_originalValue);
			}
		}

		public Component getTableCellEditorComponent(JTable jTable, Object obj, boolean isSelected, int row, int column)
		{
			if (obj instanceof Number && (((Number) obj).intValue() == ((Number) obj).floatValue()))
			{
				setText(Integer.toString(((Number) obj).intValue()));
			}
			else
			{
				if (obj != null)
				{
					setText(obj.toString());
				}
				else
				{
					setText("0");
				}
			}

			d_originalValue = getText();
			jTable.setRowSelectionInterval(row, row);
			jTable.setColumnSelectionInterval(column, column);
			this.setAlignmentX(RIGHT_ALIGNMENT);
			selectAll();

			return this;
		}

		public void addCellEditorListener(CellEditorListener cellEditorListener)
		{
			d_listeners.add(cellEditorListener);
		}

		public void cancelCellEditing()
		{
			fireEditingCanceled();
		}

		public void removeCellEditorListener(CellEditorListener cellEditorListener)
		{
			d_listeners.remove(cellEditorListener);
		}

		public boolean shouldSelectCell(EventObject eventObject)
		{
			return true;
		}

		public boolean stopCellEditing()
		{
			fireEditingStopped();

			return true;
		}

		private void fireEditingCanceled()
		{
			setText(d_originalValue);

			ChangeEvent ce = new ChangeEvent(this);

			for (int i = d_listeners.size() - 1; i >= 0; --i)
			{
				((CellEditorListener) d_listeners.get(i)).editingCanceled(ce);
			}
		}

		private void fireEditingStopped()
		{
			ChangeEvent ce = new ChangeEvent(this);

			for (int i = d_listeners.size() - 1; i >= 0; --i)
			{
				((CellEditorListener) d_listeners.get(i)).editingStopped(ce);
			}
		}
	}

	/**
	 * This class is an ActionListener used to perform a sale of the
	 * equipment item currently selected in the selected table at the
	 * rate selected in the buy rate combo box.
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private class SellGearActionListener extends SelectedGearActionListener
	{
		SellGearActionListener(int qty)
		{
			super(qty);
		}

		protected void updateEquipment(Equipment eq, int argQty)
		{
			if (argQty < -1)
			{
				sellSpecifiedEquipment(eq, eq.qty());
			}
			else
			{
				sellSpecifiedEquipment(eq, argQty);
			}
		}
	}

	/**
	 * This class is an ActionListener used to perform a sale of the
	 * equipment item currently selected in the selected table at the
	 * rate selected at the time the action is performed.
	 *
	 * sage_sam 27 Feb 2003 for FREQ 606205
	 */
	private class SellRateGearActionListener extends SelectedGearActionListener
	{
		SellRateGearActionListener(int qty)
		{
			super(qty);
		}

		protected void updateEquipment(Equipment eq, int argQty)
		{
			if (argQty < -1)
			{
				sellSpecifiedEquipmentRate(eq);
			}
			else
			{
				sellSpecifiedEquipmentRate(eq, argQty);
			}
		}
	}
}

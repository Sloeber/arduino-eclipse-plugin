/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: AvariceProgrammers.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets.tools;

import it.baeyens.avreclipse.core.targets.ClockValuesGenerator;
import it.baeyens.avreclipse.core.targets.HostInterface;
import it.baeyens.avreclipse.core.targets.IProgrammer;
import it.baeyens.avreclipse.core.targets.TargetInterface;
import it.baeyens.avreclipse.core.targets.ClockValuesGenerator.ClockValuesType;

/**
 * Enumeration of all Programmers supported by avarice.
 * <p>
 * Unlike avrdude the avarice application has no command line argument to list all supported
 * programmers, so this list is hard-coded on the assumption that avarice won't get support for new
 * devices to often.
 * </p>
 * <p>
 * This enumeration implements the {@link IProgrammer} interface, so that its members can be
 * directly used by {@link AvariceTool}.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public enum AvariceProgrammers implements IProgrammer {

	dragon_jtag("Atmel AVR Dragon in JTAG mode") {

		@Override
		public HostInterface[] getHostInterfaces() {
			return new HostInterface[] { HostInterface.USB };
		}

		@Override
		public TargetInterface getTargetInterface() {
			return TargetInterface.JTAG;
		}

		@Override
		public int[] getTargetInterfaceClockFrequencies() {
			return ClockValuesGenerator.getValues(ClockValuesType.JTAG2);
		}

		@Override
		public boolean isDaisyChainCapable() {
			return true;
		}

	},

	dragon_dw("Atmel AVR Dragon in debugWire mode") {

		@Override
		public HostInterface[] getHostInterfaces() {
			return new HostInterface[] { HostInterface.USB };
		}

		@Override
		public TargetInterface getTargetInterface() {
			return TargetInterface.DW;
		}

		@Override
		public int[] getTargetInterfaceClockFrequencies() {
			return ClockValuesGenerator.getValues(ClockValuesType.JTAG2);
		}

		@Override
		public boolean isDaisyChainCapable() {
			return false;
		}

	},

	jtag1("Atmel JTAG ICE (mkI)") {

		@Override
		public HostInterface[] getHostInterfaces() {
			return new HostInterface[] { HostInterface.SERIAL, HostInterface.USB };
		}

		@Override
		public TargetInterface getTargetInterface() {
			return TargetInterface.JTAG;
		}

		@Override
		public int[] getTargetInterfaceClockFrequencies() {
			return ClockValuesGenerator.getValues(ClockValuesType.JTAG1);
		}

		@Override
		public boolean isDaisyChainCapable() {
			return true;
		}

	},

	jtag2("Atmel JTAG ICE mkII") {

		@Override
		public HostInterface[] getHostInterfaces() {
			return new HostInterface[] { HostInterface.SERIAL, HostInterface.USB };
		}

		@Override
		public TargetInterface getTargetInterface() {
			return TargetInterface.JTAG;
		}

		@Override
		public int[] getTargetInterfaceClockFrequencies() {
			return ClockValuesGenerator.getValues(ClockValuesType.JTAG2);
		}

		@Override
		public boolean isDaisyChainCapable() {
			return true;
		}

	},

	jtag2dw("Atmel JTAG ICE mkII in debugWire mode") {

		@Override
		public HostInterface[] getHostInterfaces() {
			return new HostInterface[] { HostInterface.SERIAL, HostInterface.USB };
		}

		@Override
		public TargetInterface getTargetInterface() {
			return TargetInterface.DW;
		}

		@Override
		public int[] getTargetInterfaceClockFrequencies() {
			return ClockValuesGenerator.getValues(ClockValuesType.JTAG2);
		}

		@Override
		public boolean isDaisyChainCapable() {
			return false;
		}

	};

	private String	fDescription;

	private AvariceProgrammers(String description) {
		fDescription = description;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IProgrammer#getId()
	 */
	public String getId() {
		return this.name();
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IProgrammer#getDescription()
	 */
	public String getDescription() {
		return fDescription;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IProgrammer#getAdditionalInfo()
	 */
	public String getAdditionalInfo() {
		// Avarice does not have any additional infos.
		return "";
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IProgrammer#getHostInterfaces()
	 */
	public abstract HostInterface[] getHostInterfaces();

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IProgrammer#getTargetInterface()
	 */
	public abstract TargetInterface getTargetInterface();

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IProgrammer#getTargetInterfaceClockFrequencies()
	 */
	public abstract int[] getTargetInterfaceClockFrequencies();

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IProgrammer#isDaisyChainCapable()
	 */
	public abstract boolean isDaisyChainCapable();

}

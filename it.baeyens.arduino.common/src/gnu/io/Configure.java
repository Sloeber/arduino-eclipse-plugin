/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2006 by Trent Jarvi taj@www.linux.org.uk.
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Library General Public
|   License as published by the Free Software Foundation; either
|   version 2 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Library General Public License for more details.
|
|   You should have received a copy of the GNU Library General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
--------------------------------------------------------------------------*/
package gnu.io;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

class Configure extends Frame
{
	Checkbox cb[];
	Panel p1;
	static final int PORT_SERIAL	=	1;
	static final int PORT_PARALLEL	=	2;
	int PortType = PORT_SERIAL;

	private void saveSpecifiedPorts()
	{
		String filename;
		String javaHome= new String(System.getProperty( "java.home" ) );
		String pathSep = System.getProperty( "path.separator", ":" );
		String fileSep = System.getProperty( "file.separator", "/" );
		String lineSep = System.getProperty( "line.separator" );
		String output;

		if( PortType == PORT_SERIAL )
			filename = new String( javaHome +
				fileSep + "lib" + fileSep +
				"gnu.io.rxtx.SerialPorts" );
		else if ( PortType == PORT_PARALLEL )
			filename = new String( javaHome +
				"gnu.io.rxtx.ParallelPorts" );
		else
		{
			System.out.println( "Bad Port Type!" );
			return;
		}
		System.out.println(filename);

		try {
			FileOutputStream out = new FileOutputStream( filename );

			for( int i = 0; i < 128; i++)
			{
				if( cb[i].getState() )
				{
					output = new String( cb[i].getLabel() +
						pathSep );
					out.write( output.getBytes() );
				}
			}
			out.write(lineSep.getBytes());
			out.close();
		}
		catch ( IOException e )
		{
			System.out.println("IOException!");
		}
	}

	void addCheckBoxes( String PortName )
	{
		for ( int i = 0; i < 128 ; i++ )
			if( cb[i] != null )
				p1.remove( cb[i] );
		for (int i=1;i<129;i++)
		{
			cb[i-1]=new Checkbox(PortName+i);
			p1.add( "NORTH", cb[i-1] );
		}
	}

	public Configure()
	{
		int Width= 640;
		int Height= 480;
		cb = new Checkbox[128];
		final Frame f = new Frame(
			"Configure gnu.io.rxtx.properties");
		String fileSep = System.getProperty( "file.separator", "/" );
		String devPath;
		if( fileSep.compareTo( "/" ) != 0 )
			devPath="COM";
		else
			devPath="/dev/";
			
		f.setBounds(100,50,Width,Height);
		f.setLayout(new BorderLayout());
		p1 = new Panel();
		p1.setLayout(new GridLayout(16,4));
		ActionListener l = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				{
					String res = e.getActionCommand();
					if ( res.equals( "Save" ) )
						saveSpecifiedPorts();
				}
			}
		};

		addCheckBoxes( devPath );
		TextArea t = new TextArea( EnumMessage, 5, 50, 
						TextArea.SCROLLBARS_NONE );
		t.setSize(50,Width);
		t.setEditable(false);

		final Panel p2 = new Panel();
		p2.add(new Label("Port Name:"));
		TextField tf = new TextField(devPath, 8);
		tf.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				addCheckBoxes(e.getActionCommand());
				f.setVisible(true);
			}
		});
		p2.add(tf);
		Checkbox Keep = new Checkbox("Keep Ports");
		p2.add(Keep);
		Button b[] = new Button[6];
		for(int j=0, i = 4;i<129;i*=2, j++)
		{
			b[j] = new Button("1-" + i);
			b[j].addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e )
				{
					int k = Integer.parseInt(
					e.getActionCommand().substring(2));
					for(int x = 0; x < k; x++)
					{
						cb[x].setState(
							!cb[x].getState());
						f.setVisible(true);
					}
				}
			});
			p2.add(b[j]);
		}
		Button b1 = new Button("More");
		Button b2 = new Button("Save");
		b1.addActionListener(l);
		b2.addActionListener(l);
		p2.add(b1);
		p2.add(b2);
		f.add("South", p2);
		f.add("Center", p1);
		f.add("North", t);
		f.addWindowListener(
			new WindowAdapter() {
				public void windowClosing( WindowEvent e )
				{
					System.exit( 0 );
				}
			}
		);
		f.setVisible(true);
	}
	public static void main (String[] args)
	{
		new Configure();
	}
	String EnumMessage = new String( "gnu.io.rxtx.properties has not been detected.\n\nThere is no consistant means of detecting ports on this operating System.  It is necessary to indicate which ports are valid on this system before proper port enumeration can happen.  Please check the ports that are valid on this system and select Save");
}


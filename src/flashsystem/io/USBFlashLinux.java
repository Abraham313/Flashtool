package flashsystem.io;

import flashsystem.S1Packet;
import flashsystem.X10FlashException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import win32lib.JKernel32;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

import linuxlib.JUsb;

public class USBFlashLinux {
	
	private static int lastflags;
	private static byte[] lastreply;
	private static Logger logger = Logger.getLogger(USBFlashLinux.class);
	private static int usbbuffer=4*(512*1024);
	
	public static void setUSBBuffer(int buffer) {
		usbbuffer=buffer;
	}

	public static void linuxOpen(String pid) throws IOException, Exception  {
			logger.info("Opening device for R/W");
			JUsb.fillDevice(false);
			JUsb.open();
			logger.info("Device ready for R/W.");
	}

	public static boolean linuxWriteS1(S1Packet p) throws IOException,X10FlashException {
		try {
		if (p.getDataLength()>usbbuffer) {
			JUsb.writeBytes(p.getHeaderWithChecksum());
			JBBPBitInputStream dataStream = new JBBPBitInputStream(new ByteArrayInputStream(p.getDataArray()));
			int sent=0;
			while (dataStream.hasAvailableData()) {
				if (sent+usbbuffer>p.getDataLength()) {
					byte[] buf = dataStream.readByteArray(p.getDataLength()-sent);
					JUsb.writeBytes(buf);
				}
				else {
					byte[] buf = dataStream.readByteArray(usbbuffer);
					JUsb.writeBytes(buf);
					sent+=usbbuffer;
				}
			}
			JUsb.writeBytes(p.getCRC32());
		}
		else
			JUsb.writeBytes(p.getByteArray());
		logger.debug("OUT : " + p);
		return true;
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	public static void linuxWrite(byte[] array) throws IOException,X10FlashException {
		try {
			JUsb.writeBytes(array);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public static  void linuxReadS1Reply() throws X10FlashException, IOException
    {
    	logger.debug("Reading packet from phone");
    	S1Packet p=new S1Packet(JUsb.readBytes());
    	while (p.hasMoreToRead()) {
    		p.addData(JUsb.readBytes());
    	}
		p.validate();
		logger.debug("IN : " + p);
		lastreply = p.getDataArray();
		lastflags = p.getFlags();
    }

    public static void linuxReadReply()  throws X10FlashException, IOException {
    	try {
			lastreply = JUsb.readBytes(0x10000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static int linuxGetLastFlags() {
    	return lastflags;
    }
    
    public static byte[] linuxGetLastReply() {
    	return lastreply;
    }

    public static void linuxClose() {
		try {
			JUsb.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
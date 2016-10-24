/*
 * This file is part of MyDMAM / On Road Media Send.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileValidation {
	
	public static void checkExistsCanRead(File element) throws IOException, NullPointerException {
		if (element == null) {
			throw new NullPointerException("element is null"); //$NON-NLS-1$
		}
		if (element.exists() == false) {
			throw new FileNotFoundException("\"" + element.getPath() + "\" in filesytem"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (element.canRead() == false) {
			throw new IOException("Can't read element \"" + element.getPath() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * @return true if element is FS side hidden or element name start by a "."
	 */
	public static boolean isHidden(File element) {
		if (element == null) {
			return false;
		}
		if (element.isHidden()) {
			return true;
		}
		if (element.getName().startsWith(".")) { //$NON-NLS-1$
			return true;
		}
		return false;
	}
	
	public static void checkIsDirectory(File element) throws FileNotFoundException {
		if (element.isDirectory() == false) {
			throw new FileNotFoundException("\"" + element.getPath() + "\" is not a directory"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public static void checkIsWritable(File element) throws IOException {
		if (element.canWrite() == false) {
			throw new IOException("\"" + element.getPath() + "\" is not writable"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public static void checkCanExecute(File element) throws IOException, NullPointerException {
		if (element == null) {
			throw new NullPointerException("element is null"); //$NON-NLS-1$
		}
		if (element.exists() == false) {
			throw new FileNotFoundException("\"" + element.getPath() + "\" in filesytem"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (element.canExecute() == false) {
			throw new IOException("Can't execute element \"" + element.getPath() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
}

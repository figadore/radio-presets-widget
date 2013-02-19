/*
 * Copyright (C) 2013 Reese Wilson | Shiny Mayhem

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.shinymayhem.radiopresets;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class Logger extends Activity{
	
	public void log(Object caller, String text)
	{
		log(caller, text, "v");
	}
	
	public void log(Context context, String callerClass, String text, String level)
	{
		String str = text;
		FileOutputStream file;
		if (level == "v")
		{
			str = "VERBOSE:\t\t" + str;
			Log.v(callerClass, str);
		}
		else if (level == "d")
		{
			str = "DEBUG:\t\t" + str;
			Log.d(callerClass, str);
		}
		else if (level == "i")
		{
			str = "INFO:\t\t" + str;
			Log.i(callerClass, str);
		}
		else if (level == "w")
		{
			str = "WARN:\t\t" + str;
			Log.w(callerClass, str);
		}
		else if (level == "e")
		{
			str = "ERROR:\t\t" + str;
			Log.e(callerClass, str);
		}
		else
		{
			Log.e(callerClass, "new log level");
			str = level + str;
			Log.e(callerClass, str);
		}
		
		try {
			str = callerClass + ":\t\t" + str;
			Calendar cal = Calendar.getInstance();
	    	cal.getTime();
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	    	str += "\n";
	    	str = sdf.format(cal.getTime()) + "\t\t" + str;
			file = context.openFileOutput(MainActivity.LOG_FILENAME, Context.MODE_APPEND);
			file.write(str.getBytes());
			file.flush();
			file.close();
			//TODO switch to temp files before field testing
			//file = File.createTempFile(fileName, null, this.getCacheDir());
			//file.
		}
		catch (Exception e)
		{
	    	//Toast.makeText(this, "error writing to log file", Toast.LENGTH_SHORT).show();
	    	e.printStackTrace();
		}
	}
	
	public void log(Object caller, String text, String level)
	{
		String callerClass = caller.getClass().toString();
		String str = text;
		FileOutputStream file;
		if (level == "v")
		{
			str = "VERBOSE:\t\t" + str;
			Log.v(callerClass, str);
		}
		else if (level == "d")
		{
			str = "DEBUG:\t\t" + str;
			Log.d(callerClass, str);
		}
		else if (level == "i")
		{
			str = "INFO:\t\t" + str;
			Log.i(callerClass, str);
		}
		else if (level == "w")
		{
			str = "WARN:\t\t" + str;
			Log.w(callerClass, str);
		}
		else if (level == "e")
		{
			str = "ERROR:\t\t" + str;
			Log.e(callerClass, str);
		}
		else
		{
			Log.e(callerClass, "new log level");
			str = level + str;
			Log.e(callerClass, str);
		}
		
		try {
			str = callerClass + ":\t\t" + str;
			Calendar cal = Calendar.getInstance();
	    	cal.getTime();
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	    	str += "\n";
	    	str = sdf.format(cal.getTime()) + "\t\t" + str;
	    	Context context = (Context)caller;
			file = context.openFileOutput(MainActivity.LOG_FILENAME, Context.MODE_APPEND);
			file.write(str.getBytes());
			file.flush();
			file.close();
			//TODO switch to temp files before field testing
			//file = File.createTempFile(fileName, null, this.getCacheDir());
			//file.
		}
		catch (Exception e)
		{
	    	//Toast.makeText(this, "error writing to log file", Toast.LENGTH_SHORT).show();
	    	e.printStackTrace();
		}
	}
	
	
}

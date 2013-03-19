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
import android.os.AsyncTask;
import android.util.Log;

public class ActivityLogger extends Activity{
	
	protected Context mContext;
	
	public void log(Object caller, String text)
	{
		log(caller, text, "v");
		
	}
	
	public void log(String tag, String text, String level)
	{
		String str = text;
		//FileOutputStream file;
		if (level == "v")
		{
			str = "VERBOSE:\t\t" + str;
			Log.v(tag, str);
		}
		else if (level == "d")
		{
			str = "DEBUG:\t\t" + str;
			Log.d(tag, str);
		}
		else if (level == "i")
		{
			str = "INFO:\t\t" + str;
			Log.i(tag, str);
		}
		else if (level == "w")
		{
			str = "WARN:\t\t" + str;
			Log.w(tag, str);
		}
		else if (level == "e")
		{
			str = "ERROR:\t\t" + str;
			Log.e(tag, str);
		}
		else
		{
			Log.e(tag, "new log level");
			str = level + str;
			Log.e(tag, str);
		}
		FileWriterTask task = new FileWriterTask();
		str = tag + ":\t\t" + str;
		mContext = getApplicationContext();
		task.execute(str);
	}
	
	public void log(Context context, String callerClass, String text, String level)
	{
		String str = text;
		//FileOutputStream file;
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
		FileWriterTask task = new FileWriterTask();
		str = callerClass + ":\t\t" + str;
		mContext = context;
		task.execute(str);
		
		/*try {
			
			Calendar cal = Calendar.getInstance();
	    	cal.getTime();
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	    	str += "\n";
	    	str = sdf.format(cal.getTime()) + "\t\t" + str;
			file = context.openFileOutput(ActivityMain.LOG_FILENAME, Context.MODE_APPEND);
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
		}*/
	}
	
	public void log(Object caller, String text, String level)
	{
		String callerClass = caller.getClass().toString();
		String str = text;
		
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
		FileWriterTask task = new FileWriterTask();
		str = callerClass + ":\t\t" + str;
		mContext = (Context)caller;
		task.execute(str);
		
	}
	
	public class FileWriterTask extends AsyncTask<String, Void, Void> {
		
		
		  @Override
		  protected Void doInBackground(String... params) {
			  String str = params[0];
			  FileOutputStream file;
		    // Do your filewriting here. The text should now be in params[0]
			  try {
					
					Calendar cal = Calendar.getInstance();
			    	cal.getTime();
			    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
			    	str += "\n";
			    	str = sdf.format(cal.getTime()) + "\t\t" + str;
			    	//Context context = this;//(Context)caller;
					file = mContext.openFileOutput(ActivityMain.LOG_FILENAME, Context.MODE_APPEND);
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
			  return null;
		  }
		}
	
}

package communitycommons;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.fileupload.util.LimitedInputStream;
import org.apache.commons.io.IOUtils;

import system.proxies.FileDocument;

import com.google.common.collect.ImmutableMap;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.core.MxRuntime;
import com.mendix.core.objectmanagement.member.MendixBoolean;
import com.mendix.m2ee.log.ILogNode;
import com.mendix.modules.webservices.WebserviceException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.ISession;
import com.mendix.systemwideinterfaces.core.IUser;
import communitycommons.proxies.DTAPMode;


public class Misc
{
	
	public abstract static class IterateCallback<T1, T2>
  {
      boolean start = false;
      boolean stop = false;
      private Iterator<T1>    mapIter;

      public abstract void hit(T1 key, T2 value) throws Exception;

      public void exit() {
          stop = true;
      }

      public void remove() {
          mapIter.remove();
      }

      synchronized void runOn(Map<T1, T2> map) throws Exception {
          if (start)
              throw new IllegalMonitorStateException();
          start = true;

          try {
              this.mapIter = map.keySet().iterator();

              while ( mapIter.hasNext() )
        {
            T1 key = mapIter.next();
            T2 value = map.get(key);

            hit(key, value);

            if (stop)
                break;
        }
          }

          finally {
              //reset state to allow reuse, even when exceptions occur
              mapIter = null;
              stop  = false;
              start = false;
          }
      }
  }

  /**
   * Because you cannot remove items from a map while iteration, this function is introduced.
   * In the callback, you can use this.remove() or this.exit() to either remove or break the loop. Use return; to continue
   * @throws Exception
   */
  public static <A, B> void iterateMap(Map<A, B> map, IterateCallback<A, B> callback) throws Exception {
      //http://marxsoftware.blogspot.com/2008/04/removing-entry-from-java-map-during.html
      if (map == null || callback == null)
          throw new IllegalArgumentException();

      callback.runOn(map);
  } 
	
	public static String getApplicationURL()
	{
		return Core.getConfiguration().getApplicationRootUrl();
	}

	public static String getRuntimeVersion()
	{
		Package currentPackage = MxRuntime.class.getPackage();
		String specVersion = currentPackage.getSpecificationVersion();
		String implVersion = currentPackage.getImplementationVersion();
		if(specVersion != null && implVersion != null) 
			return implVersion + " (build " + specVersion + ")"; 
		return
			"unreleased";
	}

	public static void throwException(String message) throws UserThrownException
	{
		throw new UserThrownException(message);		
	}
	
	public static void throwWebserviceException(String faultstring) throws WebserviceException {
		throw new WebserviceException(WebserviceException.clientFaultCode, faultstring);
	}

	public static String retrieveURL(String url, String postdata) throws Exception
	{
		// Send data, appname
		URLConnection conn = new URL(url).openConnection();

		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
		if (postdata != null) {
			IOUtils.copy(new ByteArrayInputStream(postdata.getBytes("UTF-8")), conn.getOutputStream());
		}
		IOUtils.closeQuietly(conn.getOutputStream());

		// Get the response
		String result = new String(IOUtils.toString(conn.getInputStream()));
		IOUtils.closeQuietly(conn.getInputStream());
		return result;
	}

	public static Boolean duplicateFileDocument(IContext context, IMendixObject toClone, IMendixObject target) throws Exception
	{
		if (toClone == null || target == null)
			throw new Exception("No file to clone or to clone into provided");
		
		 MendixBoolean hasContents = (MendixBoolean) toClone.getMember(context, FileDocument.MemberNames.HasContents.toString());
     if (!hasContents.getValue(context))
    	 return false;

		InputStream inputStream = Core.getFileDocumentContent(context, toClone); 
	
		Core.storeFileDocumentContent(context, target, (String) toClone.getValue(context, system.proxies.FileDocument.MemberNames.Name.toString()),  inputStream); 
		return true;
	}
	
  public static Boolean duplicateImage(IContext context, IMendixObject toClone, IMendixObject target, int thumbWidth, int thumbHeight) throws Exception
  {
      if (toClone == null || target == null)
          throw new Exception("No file to clone or to clone into provided");

      MendixBoolean hasContents = (MendixBoolean) toClone.getMember(context, FileDocument.MemberNames.HasContents.toString());
      if (!hasContents.getValue(context))
          return false;

      InputStream inputStream = Core.getImage(context, toClone, false); 

      Core.storeImageDocumentContent(context, target, inputStream, thumbWidth, thumbHeight);

      return true;
  }

	public static Boolean storeURLToFileDocument(IContext context, String url, IMendixObject __document, String filename) throws Exception
	{
        if (__document == null || url == null || filename == null)
            throw new Exception("No document, filename or URL provided");
        
        final int MAX_REMOTE_FILESIZE = 1024 * 1024 * 200; //maxium of 200 MB
        URL imageUrl = new URL(url);
        URLConnection connection = imageUrl.openConnection();
        //we connect in 20 seconds or not at all
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.connect();

        //check on forehand the size of the remote file, we don't want to kill the server by providing a 3 terabyte image. 
        if (connection.getContentLength() > MAX_REMOTE_FILESIZE) { //maximum of 200 mb 
            throw new IllegalArgumentException("MxID: importing image, wrong filesize of remote url: " + connection.getContentLength()+ " (max: " + String.valueOf(MAX_REMOTE_FILESIZE)+ ")");
        } else if (connection.getContentLength() < 0) {
            // connection has not specified content length, wrap stream in a LimitedInputStream
            LimitedInputStream limitStream = new LimitedInputStream(connection.getInputStream(), MAX_REMOTE_FILESIZE) {                
                @Override
                protected void raiseError(long pSizeMax, long pCount) throws IOException {
                    throw new IllegalArgumentException("MxID: importing image, wrong filesize of remote url (max: " + String.valueOf(MAX_REMOTE_FILESIZE)+ ")");                    
                }
            };
            Core.storeFileDocumentContent(context, __document, filename, limitStream);
        } else {
            // connection has specified correct content length, read the stream normally
            //NB; stream is closed by the core
            Core.storeFileDocumentContent(context, __document, filename, connection.getInputStream());
        }
        
        return true;
	}

	@SuppressWarnings("deprecation")
	public static Long getFileSize(IContext context, IMendixObject document)
	{
		long size = 0;
		if (context != null) {
			try {
				try {
					File f = Core.getFileDocumentContentAsFile(context, document);
					size =  f.length();
				}
				catch (AccessControlException e1) {
						//We seem to be in cloud security mode..., naive implementation: read the whole stream, count the bytes
						InputStream in = Core.getFileDocumentContent(context, document);
						byte[] buffer = new byte[1024];
						int i;
						while ((i = in.read(buffer)) != -1) 
						    size += i;
				}
			}
			catch(Exception e) {
				throw new RuntimeException("Unable to read filesize: " + e.getMessage(), e);
			}
		}
		return size;
	}

	public static void delay(long delaytime) throws InterruptedException
	{
		Thread.sleep(delaytime);
	}

	@Deprecated
	public static DTAPMode getDTAPMode()
	{
		return DTAPMode.valueOf(Core.getConfiguration().getDTAPMode().toString());
	}

	public static IContext getContextFor(IContext context, String username, boolean sudoContext) throws Exception {
		if (username == null || username.isEmpty())
			throw new Exception("Assertion: No username provided");
		
		ISession session  = Core.getActiveSession(username);
		if (session == null) {
			IUser user = Core.getUser(context, username);
			if (user == null)
				throw new Exception("Assertion: user with username '" + username + "' does not exist");
			session = Core.initializeSession(context, user, null, null);
		}
		
		IContext c = session.createContext();
		if (sudoContext)
			return c.getSudoContext();
			
		return c;
	}
	
	public static Object executeMicroflowAsUser(IContext context,
			String microflowName, String username, Boolean sudoContext, Object... args) throws Exception
	{
		
		if (context == null)
			throw new Exception("Assertion: No context provided");
		if (microflowName == null || microflowName.isEmpty())
			throw new Exception("Assertion: No context provided");
		if (!Core.getMicroflowNames().contains(microflowName))
			throw new Exception("Assertion: microflow not found: " + microflowName);
		if (args.length % 2 != 0)
			throw new Exception("Assertion: odd number of dynamic arguments provided, please name every argument: " + args.length);
		
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		for(int i = 0; i < args.length; i+= 2) if (args[i] != null)
			params.put(args[i].toString(), args[i + 1]);
			
		IContext c = getContextFor(context, username, sudoContext);
		
		return Core.execute(c, microflowName, params);
	}

	//MWE: based on: http://download.oracle.com/javase/6/docs/api/java/util/concurrent/Executor.html
	
	static class MFSerialExecutor implements Executor {
		
		final ExecutorService executer = Executors.newSingleThreadExecutor(new ThreadFactory() {

			final ThreadFactory tf =  Executors.defaultThreadFactory();

			@Override
			public Thread newThread(Runnable arg0)
			{
				Thread t = tf.newThread(arg0);
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
			
		});
		
		AtomicInteger tasknr = new AtomicInteger();
		private static MFSerialExecutor _instance = new MFSerialExecutor();
		
		public static MFSerialExecutor instance() {
			return _instance;
		}
		
		@Override
		public void execute(final Runnable command)
		{
			final int currentask = tasknr.incrementAndGet();
			
			final ILogNode logger = Core.getLogger("communitycommons");
			logger.info("[RunMicroflowAsyncInQueue] Scheduling task #" + currentask);
			
			//wrap the runnable in a new runnable with some logging
			
			executer.execute(new Runnable() {
				
				@Override
				public void run() {
					logger.debug("[RunMicroflowAsyncInQueue] Running task #" + currentask);
					try {
						command.run();
					} catch(Exception e) {
						logger.error("[RunMicroflowAsyncInQueue] Execution of task #" + currentask + " failed: " + e.getMessage(), e);
					}
					logger.info("[RunMicroflowAsyncInQueue] Completed task #" + currentask + " tasks.");
			}
			});
		}

	}

	public static Boolean runMicroflowAsyncInQueue(final String microflowName)
	{
		MFSerialExecutor.instance().execute( new Runnable() {
			@Override
			public void run()
			{
				try
				{
					Future<Object> future = Core.executeAsync(Core.createSystemContext(), microflowName, true, new HashMap<String,Object>()); //MWE: somehow, it only works with system context... well thats OK for now.
					future.get();
				}
				catch (Exception e)
				{
					throw new RuntimeException("Failed to run Async: "+ microflowName + ": " + e.getMessage(), e);
				}
			}
		});
		return true;
	}	
	
	public static Boolean runMicroflowInBackground(final IContext context, final String microflowName,
			final IMendixObject paramObject)
	{
		final ISession session = context.getSession();
		
		if (paramObject != null)
			session.retain(paramObject);
			
		MFSerialExecutor.instance().execute(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					IContext c = Core.createSystemContext();
					if (paramObject != null) {
							Core.executeAsync(c, microflowName, true, paramObject).get(); //MWE: somehow, it only works with system context... well thats OK for now.						
					}
					else
						Core.executeAsync(c, microflowName, true, new HashMap<String,Object>()).get(); //MWE: somehow, it only works with system context... well thats OK for now.
				}
				catch (Exception e)
				{
					throw new RuntimeException("Failed to run Async: "+ microflowName + ": " + e.getMessage(), e);
				}
				
				finally {
					if (paramObject != null)
						session.release(paramObject.getId());
				}
			}
			
		});
		return true;
	}	
	
	private interface IBatchItemHandler
	{

		void exec(IContext context, IMendixObject obj) throws Exception;

	}
	
	private static class BatchState {
		private int state = 0; //-1 = error, 1 = done.
		private IBatchItemHandler	callback;

		public BatchState(IBatchItemHandler callback) {
			this.callback = callback;
		}
		
		public void setState(int state)
		{
			this.state = state;
		}

		public int getState()
		{
			return state;
		}
		
		public void handle(IContext context, IMendixObject obj) throws Exception {
			callback.exec(context, obj);
		}
	}

	public static Boolean executeMicroflowInBatches(String xpath, final String microflow, int batchsize, boolean waitUntilFinished, boolean asc) throws CoreException, InterruptedException {
		Core.getLogger("communitycommons").info("[ExecuteInBatches] Starting microflow batch '" + microflow + "...");
		
		return executeInBatches(xpath, new BatchState(new IBatchItemHandler() {

			@Override
			public void exec(IContext context, IMendixObject obj) throws Exception
			{
				Core.executeAsync(context, microflow, true, obj).get();
  		}
			
		}), batchsize, waitUntilFinished, asc);
	}
	
	public static Boolean recommitInBatches(String xpath, int batchsize,
			boolean waitUntilFinished, Boolean asc) throws CoreException, InterruptedException
	{
		Core.getLogger("communitycommons").info("[ExecuteInBatches] Starting recommit batch...");
		
		return executeInBatches(xpath, new BatchState(new IBatchItemHandler() {

			@Override
			public void exec(IContext context, IMendixObject obj) throws Exception
			{
				Core.commit(context, obj);				
			}
			
		}), batchsize, waitUntilFinished, asc);
	}	
	
	public static Boolean executeInBatches(String xpathRaw, BatchState batchState, int batchsize, boolean waitUntilFinished, boolean asc) throws CoreException, InterruptedException
	{
		String xpath = xpathRaw.startsWith("//") ? xpathRaw : "//" + xpathRaw;
		
		long count = Core.retrieveXPathQueryAggregate(Core.createSystemContext(), "count(" + xpath + ")");
		int loop = (int) Math.ceil(((float)count) / ((float)batchsize));
		
		
		Core.getLogger("communitycommons").info(
				"[ExecuteInBatches] Starting batch on ~ " + count + " objects divided over ~ " + loop + " batches. "
				+ (waitUntilFinished ? "Waiting until the batch has finished..." : "")
		);
		
		executeInBatchesHelper(xpath, batchsize, 0, batchState, count, asc);
		
		if (waitUntilFinished) {
			while (batchState.getState() == 0) {
				Thread.sleep(5000);
			}
			if (batchState.getState() == 1) { 
				Core.getLogger("communitycommons").debug("[ExecuteInBatches] Successfully finished batch");
				return true;
			}
			Core.getLogger("communitycommons").error("[ExecuteInBatches] Failed to finish batch. Please check the application log for more details.");
			return false;
		}
		
		return true;
	}
	
	static void executeInBatchesHelper(final String xpath, final int batchsize, final long last, final BatchState batchState, final long count, final boolean asc) {
		MFSerialExecutor.instance().execute(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					Thread.sleep(200);
					IContext c = Core.createSystemContext();
					
					List<IMendixObject> objects = Core.retrieveXPathQuery(c, xpath + (last > 0 ? "[id " + (asc ? "> " : "< ") + last + "]" : ""), batchsize, 0, ImmutableMap.of("id", asc ? "asc" : "desc"));
					
					//no new objects found :)
					if (objects.size() == 0) {
						Core.getLogger("communitycommons").info("[ExecuteInBatches] Succesfully finished batch on ~" + count + " objects.");
						batchState.setState(1);
					}
					else {
						
						//process objects
						for(IMendixObject obj: objects)
							batchState.handle(c, obj);
						
						//invoke next batch
						executeInBatchesHelper(xpath, batchsize, objects.get(objects.size() - 1).getId().getGuid(), batchState, count, asc);
					}
				}
				catch (Exception e)
				{
					batchState.setState(-1);
					throw new RuntimeException("[ExecuteInBatches] Failed to run in batch: " + e.getMessage(), e);
				}
			}
			
		});
	}
	
	/**
	 * Tests if two objects are equal with throwing unecessary null pointer exceptions. 
	 * 
	 * This is almost the most stupid function ever, since it should be part of Java itself. 
	 * 
	 * In java 7 it will finally be available as static method Object.equals()
	 * @param left
	 * @param right
	 * @return
	 */
	public static boolean objectsAreEqual(Object left, Object right) {
		if (left == null && right == null)
			return true;
		if (left == null || right == null)
			return false;
		return left.equals(right);
	}
	
}

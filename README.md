dli-downloader
==============

Fast tool to Download Free eBooks in PDF format from Digital Library of India

DLI hosts thousands of CopyRight? Freed eBooks which users can view and download for their own use. But unfortunately DLI does not provide us with the option to download the complete e-books in PDF format to our computer.

Digital Library of India http://dli.gov.in/

Free Books Collection Status http://www.dli.gov.in/cgi-bin/status.cgi

DLI Downloader Blog
http://dli-downloader.blogspot.in/

###How does it work (Windows 8 x64)?
**5 Minutes Video Introduction to the Tool**

<a href="http://www.youtube.com/watch?feature=player_embedded&v=aV4eJiX7rys
" target="_blank"><img src="http://img.youtube.com/vi/aV4eJiX7rys/0.jpg" 
alt="IMAGE ALT TEXT HERE" width="400" height="350" border="10" /></a>

**How to Enable Local Search for a Language**

<a href="http://www.youtube.com/watch?feature=player_embedded&v=qYXsOV8NO5k
" target="_blank"><img src="http://img.youtube.com/vi/qYXsOV8NO5k/0.jpg" 
alt="IMAGE ALT TEXT HERE" width="400" height="350" border="10" /></a>

###Key Features
  * Search/Download/Open DLI eBooks in PDF Format right within the Tool , no need to go to any DLI servers evers !
  * Load balancing your download from multiple dli-servers for better speed.
  * 100% Network Fault Tolerance - Downloads will not fail if the internet connection is disrupted, dli-downloader will silently wait for the connection restore.
  * Queue Upto 100 of your e-book barcodes and this tool will download them silently for you over the time (batching of downloads).
  * Option to control the download bandwidth of the tool so that it does not eat up all the data alone.
  * Bulk download barcodes by providing the text file which contains all the requested barcodes delimited by the newline, comma or semicolon
  * Upon application shutdown, it will save the queued up barcodes so that the downloads can be started again on next launch
  * Application can minimize to the System Tray and thus keep running in background
  * Works well on the Windows 7/8 PC, Ubuntu 12.10 and Mac, on both x86 and x64 OS'es

###Searching & Downloading an eBook
  * If you want to search all hindi books then type hindi
  * If you do not know exact spell of the word then append ~ at the end like handi~ will search hindi books as well
  * If you want to search all books which are not hindi then type criteria -hindi
  * If you got the desired book in the listing then right click on it and choose download
  * If you got the desired book and want to search books similar books then right click and choose Find Similar
  * If you have the local copy of the eBook, then it will appear in green color.
   
###System Requirements & Installation Steps
  * Windows XP Windows 7 Windows 8, Mac, Ubuntu 12.10, x86 & x64
  * Download Java 7 runtime from oracle website and install it on your windows PC. http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html
  * Download the binary jar file from the Downloads section of this project and save it on your computer.
  * Run the jar by double-clicking it in the windows environment. UNIX and Mac users please run the following command (or associate with Java Runtime for the first use)
   java -jar dli-downloader-5.2-jar-with-dependencies
  * A GUI will appear once you run the app, and System Tray will now have one icon for controlling this DLI downloader.
  * Create a Local Index for a particular language books, that may take few minutes.
  * Search/Download/Open books in search Tab after the previous step is over.
  * Once you are done downloading the e-book, goto System Tray and find the DLI icon, right click on it and choose exit.

==Configuration Settings==
Go to System Tray and right click on the DLI icon and then select Settings, a popup window will appear as shown below -
![DLI settings](http://dli-downloader.googlecode.com/svn/Settings.png "DLI Settings Tab")

Now you can edit the config values in this editor after double clicking a particular value. Please restart the application after making the necessary changes, otherwise the effects will not be visible.

###Configuration Settings Explained
 * *Quality* - TIFF to PDF conversion quality. A2 is maximum, A7 is minimum. Default value is A2 if not specified.
 * *numberParallelJobs* - number of parallel downloads at any given point in time.
 * *readTimeOutMs* - this is socket read time out value before throwing an timeout exception in milli seconds.
 * *rootDirectory* - directory where the fresh downloads will be saved.
 * speedLimitKBps - maximum speed limit that the application will use for all its downloads.
 * *deleteTifIfSuccessful* - if set to true then the TIFF files will be deleted if PDF conversion and tiff download goes successful.
 * *maxRetryCount* - If connection to server is very slow then the connection timeout exception can occur. In case the download fails, then this property will decide how many times to retry the failed download.
 * *maxConsecutiveFailure* - maximum number of consecutive IOExceptions from the server before the tool stops downloading the given barcode.
 * *downloadDirectories* - comma or semicolon separated list of directories which can contain the already downloaded PDF files. The pdf file must contain the barcode in its filename. This directory will be scanned by the tool at the time of startup for caching the existing downloads and a warning prompt will appear to user if he tries to download the same barcode again.
 * *lookAndFeelNumber* - 0 is SystemLookAndFeel, 1 is NimbusLookAndFeel, 2 is WindowsLookAndFeel, 3 is GTKLookAndFeel, 4 is MotifLookAndFeel
 * *createBarcodePage* - true will create first page in output PDF displaying the metadata of the e-book. False will do not create any initial page.

###Donation - Please support us for the cause (Rs 100 atleast)
If anyone feel like donating money as little as *Rs 100* for this work, feel free to do that. Every Rs donated will be used for the charity work. So it helps a lot!!

###Where do we spend our Funds ?
http://dli-downloader.blogspot.in/2013/12/charity-for-prosperity-where-fund-goes.html

**Bank Deposit by NEFT**

MUNISH CHANDEL

a/c 5277618224

IFSC CITI0000002

Citi Bank N.A. Delhi

Don't forget to send me the details on email : cancerian0684@gmail.com

####About the Author
For any feedback/suggestions please write back to me at cancerian0684@gmail.com or arunsharma.nith@gmail.com

Class CheckVMotionOccurred
{
  private boolean getIsVMotionOccurred(String hostName, String port, String userName, String password, String vmName) 
	{
		ServiceInstance serviceInstance = null;
		try {
			StringBuffer url = new StringBuffer("https");//No I18N
			url.append("://");
			url.append(hostName);
			url.append(":");
			url.append(port);
			url.append("/sdk");//No I18N
			String urlString = url.toString();
			serviceInstance = VMWareAPIUtil.getServiceInstance(new URL(urlString), userName, password, true);
			
			TaskManager taskMgr = serviceInstance.getTaskManager();
			if(taskMgr == null) {
				serviceInstance.getServerConnection().logout();
				return false;
			}
			TaskFilterSpec tfs = new TaskFilterSpec();
			
			//only the task with current VM Entity
			/*TaskFilterSpecByEntity entFilter = new TaskFilterSpecByEntity();
			Folder rootFolder = serviceInstance.getRootFolder();
			VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder)
					.searchManagedEntity("VirtualMachine", vmName);//No I18N
			entFilter.setEntity(vm.getMOR());
			entFilter.setRecursion(TaskFilterSpecRecursionOption.all);
			tfs.setEntity(entFilter);*/
			
			// only successfully finished tasks
			//tfs.setState(new TaskInfoState[]{TaskInfoState.success });
			
			// only tasks started within VMOTION_INTERVAL_MILLIS
			TaskFilterSpecByTime tFilter = new TaskFilterSpecByTime();
			
			/*Calendar cal = serviceInstance.currentTime();
			System.out.println("Initial Server time : "+cal.getTime().toString());
			cal.setTimeInMillis(cal.getTimeInMillis() - VMOTION_INTERVAL_MILLIS);
			System.out.println("Updated Server time : "+cal.getTime().toString());
			tFilter.setBeginTime(cal); //we ignore the end time here so it gets the latest.
			 */
			
			Calendar cal = Calendar.getInstance();
			System.out.println("Initial Server time : "+cal.getTime().toString());
			cal.roll(Calendar.MONTH, -1);
			System.out.println("Updated Server time : "+cal.getTime().toString());
			tFilter.setBeginTime(cal);
			
			tFilter.setTimeType(TaskFilterSpecTimeOption.startedTime);
			tfs.setTime(tFilter);
			
			TaskHistoryCollector thc = taskMgr.createCollectorForTasks(tfs);
			//thc.rewindCollector();
			//int tasknumber = 999; // Windowsize for task collector
			//TaskInfo[] taskInfo = thc.readNextTasks(tasknumber);
			thc.setCollectorPageSize(15);
			TaskInfo[] taskInfo = thc.getLatestPage();
			
			if(taskInfo == null) {
				serviceInstance.getServerConnection().logout();
				return false;
			}
			
			do {
				for(TaskInfo task : taskInfo) {
					String desc = task.getDescriptionId();
					System.out.println(task.getName()+" : "+task.getDescriptionId());
					if(desc.equalsIgnoreCase("VirtualMachine.migrate") || desc.equalsIgnoreCase("Drm.ExecuteVMotionLRO")) {
						thc.destroyCollector();
						serviceInstance.getServerConnection().logout();
						return true;
					}
				}
				//taskInfo = thc.readNextTasks(tasknumber);
			}while (taskInfo != null && taskInfo.length > 0);
			System.out.println("Out of Tasks");
		}
		catch(Throwable e) {
			e.printStackTrace();
			
			if(serviceInstance != null) {
				serviceInstance.getServerConnection().logout();
			}
		}
		serviceInstance.getServerConnection().logout();
		return false;
	}
  
  public static void main(String[] args) {
    
  }
}

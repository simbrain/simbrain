The three build_ scripts pull the jars, etc., package them with whatever special items are needed for a particular distribution, zip them up, and place them in the public_html directory.  The build_all.sh script calls these scripts and sends them to the server.

Note 1:  Prior to invoking these scripts the "build" ant task in the previous directory must be invoked.

Note 2: The task used to create the windows executible relies on "roxes" ant tasks, and requires that roxes-ant-tasks-1.2-2004-01-30.jar or its equivalent be palced in your ant/lib directory. 

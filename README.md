In resources folder add youtube API key;
In order to run, its easier to run with wrapper which is included, command: 
 for unix: ./mvnw spring-boot:run,
 for windows: mvnw.cmd spring-boot:run


how it works:
In order to minimize requests to youtube API we are managing trending video and commenters by country, which is updated every 15 minutes(youtube trending video information is updated roughly every 15 minutes), we are saving last request time for user, in order to avoid making unnecessary API calls, if difference between last request time and current request time is more then last update time(time which was set for user on registration, to update video and comment), we take it from cron which is running every 15 minutes, for countries.Otherwise we take it from database, but in case new user joins, and we already have trending video up to date, we no longer make API request call, we get it by country.
This way, we are no longer making unnecessary API calls, we call it when we have active user for current country, and if all users disconnect with specific country, we stop cron for that specific country;

front-end sends new request for trending video every x seconds(x is user specific update time), and sends logout api call, so we know that if we no longer have users for specific country, we should stop cron for that country;

package com.wakeup;


import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmService extends IntentService {
    public static final String DOWHATNEED = "DOWHATNEED";
    public static final String DELETE = "DELETE";
    final static String myLog = "myLog";
    private IntentFilter matcher;
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TIME_HOUR = "timeHour";
    public static final String TIME_MINUTE = "timeMinute";
    public static final String TONE = "alarmTone";
    public static final String ACTION_ALARM_CHANGED = "android.intent.action.ALARM_CHANGED";
    ArrayList<Alarm> alarms = new ArrayList<Alarm>();
    ArrayList<String> repeatDaysList;
    AlarmManager alarmManager;
    DatabaseHandler db;

    public AlarmService() {
        super(null);
        matcher = new IntentFilter();
        matcher.addAction(DOWHATNEED);
        matcher.addAction(DELETE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        int needAlarm = intent.getIntExtra("id", 999);

        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        db = new DatabaseHandler(this);//переменная для работы с БД


        if (matcher.matchAction(action)) {//если пеереданное значение совпадает с создать или отменить
          if(action.equals(DOWHATNEED)){
              execute(action);
          }else if(action.equals(DELETE)){
              deleteAlarm(needAlarm);
          }
        }
    }

    private void deleteAlarm(int alarmId){
        Log.d(myLog, "id для удаления будильника = " + alarmId);
        if(alarmId == -1){//еслии выбрано удаление всех будильников(вередан id = -1)
            fillData();
            for (Alarm alarm : alarms) {
                stopAlarm(alarm);
                db.deleteAlarm(alarm);//запускаем метод "удаление" и передаем обьект ЗАПИСЬ со всеми полями
            }
        }else {// если задано удаление конкретного будильника
            Alarm alarm = db.getAlarmById(alarmId);
            stopAlarm(alarm);
            db.deleteAlarm(alarm);//запускаем метод "удаление" и передаем обьект ЗАПИСЬ со всеми полями
        }
    }

    public void stopAlarm(Alarm alarm){
        Intent intentForAlarmReceiver = new Intent(this, AlarmReceiver.class);
        intentForAlarmReceiver.putExtra(ID, alarm.getID());
        intentForAlarmReceiver.putExtra(TIME_HOUR, alarm.get_delayHour());
        intentForAlarmReceiver.putExtra(TIME_MINUTE, alarm.get_delayMinute());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) alarm.getID(), intentForAlarmReceiver,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);//отключаем будильник
    }

    private void execute(String action) {
        fillData();
        int numberOfDays = 0;

        for (Alarm alarm : alarms) {
            numberOfDays = 0;
            long time = 0;
            Intent intentForAlarmReceiver = new Intent(this, AlarmReceiver.class);
            intentForAlarmReceiver.putExtra(ID, alarm.getID());
            intentForAlarmReceiver.putExtra(TIME_HOUR, alarm.get_delayHour());
            intentForAlarmReceiver.putExtra(TIME_MINUTE, alarm.get_delayMinute());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) alarm.getID(), intentForAlarmReceiver,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(Calendar.MONDAY);

            Log.d(myLog, " строка повтора по дням = " + alarm.get_repetDays());
            Log.d(myLog, " первый элемент строки = " + Integer.decode(String.valueOf(alarm.get_repetDays().charAt(0))));
            Log.d(myLog, " активность будильника = " + alarm.get_active());

            calendar.set(Calendar.HOUR_OF_DAY, alarm.get_delayHour());
            calendar.set(Calendar.MINUTE, alarm.get_delayMinute());
            calendar.set(Calendar.SECOND, 00);

            if ((Integer.decode(String.valueOf(alarm.get_repetDays().charAt(0))) > 0) && (!(alarm.get_repetDays().equals("100")))) {
                    repeatDaysList = splitLine(alarm.get_repetDays());// получаем массив с днями
                    int t = -1;// переменная для подсчета необходимого дня из списка
                    int i = -1;// переменная, указывающая на день (позицию в массиве), который следующий для срабатывания

                    if ((calendar.getTime().getHours() < alarm.get_hour()) && (calendar.getTime().getHours() < alarm.get_minute())) {
                        //ничего не меняем в будильнике, так как оно еще не сработал в нужное время
                    } else {

                        while (t < 0) {
                            i++;
                            if (i < repeatDaysList.size()) {// если список еще не закончен
                                t = Integer.decode(repeatDaysList.get(i)) - calendar.getTime().getDay();
                            } else {// если прошли по всему списку, и эти дни недели уже прошли, то выбираем первый пункт списка - первый день в неделе для срабатывания
                                i = 0;
                                break;
                            }
                        }
                    }

                if(calendar.getTime().getDay() == Integer.decode(repeatDaysList.get(i))) {
                    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {// ставим будильник на следующую неделю
                        if(i + 1 < repeatDaysList.size()){
                            i+= i + 1;
                            numberOfDays =  - calendar.getTime().getDay() + Integer.decode(repeatDaysList.get(i));
                        }else {
                            i = 0;
                            numberOfDays = 7 - calendar.getTime().getDay() + Integer.decode(repeatDaysList.get(i));
                        }
                        Log.d(myLog, " numberOfDays = - " + calendar.getTime().getDay() + " + " + Integer.decode(repeatDaysList.get(i)) + " = " + numberOfDays);
                    } else {
                    }
                }else if(calendar.getTime().getDay() < Integer.decode(repeatDaysList.get(i))){
                        numberOfDays =  - calendar.getTime().getDay() + Integer.decode(repeatDaysList.get(i));
                        Log.d(myLog, " numberOfDays = - " + calendar.getTime().getDay() + " + " + Integer.decode(repeatDaysList.get(i)) + " = " + numberOfDays);
                    }else {
                        numberOfDays = 7 - calendar.getTime().getDay() + Integer.decode(repeatDaysList.get(i));
                        Log.d(myLog, " numberOfDays = 7 - " + calendar.getTime().getDay() + " + " + Integer.decode(repeatDaysList.get(i)) + " = " + numberOfDays);
                    }
                    Log.d(myLog, "calendar.getTime().getDay() = " + calendar.getTime().getDay());
            }

            if (DOWHATNEED.equals(action)) {//вариант, когда программа смотрин на флаг активности будильника в БД
                if (alarm.get_active() == 1) {//если стоит АКТИВЕН
                    if(Integer.decode(String.valueOf(alarm.get_repetDays().charAt(0))) > 0){// если стоит повтор по дням
                            Log.d(myLog, " numberOfDays = " + numberOfDays);
                            Log.d(myLog, " ДО умножения дата = " + calendar.getTime().getDate() + " месяц = " + calendar.getTime().getMonth());
                            time = calendar.getTimeInMillis() + 86400000 * numberOfDays;// ставим будильник на следующие сутки, если время уже прошло
                            calendar.setTimeInMillis(time);
                            Log.d(myLog, " ПОВТОРЯЮЩИЙСЯ будильник дата = " + calendar.getTime().getDate() + " месяц = " + calendar.getTime().getMonth());
                    }else {
                        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                            time = calendar.getTimeInMillis() + 86400000 ;// ставим будильник на следующие сутки, если время уже прошло
                        } else {
                            time = calendar.getTimeInMillis();
                        }
                        calendar.setTimeInMillis(time);
                        Log.d(myLog, " ОБЫЧНЫЙ будильник дата = "+calendar.getTime().getDate()+" месяц = "+calendar.getTime().getMonth());
                    }

                    alarmManager.cancel(pendingIntent);//отключаем будильник
                    alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);//ставим его заного
                } else {//если стоит флаг НЕ АКТИВЕН
                    alarmManager.cancel(pendingIntent);//отключаем будильник
                }
                Log.d(myLog, "Время срабатывания " + alarm.getID() + " DOWHATNEED = " + time);
            }
        }
    }


    public ArrayList<String> splitLine(String lineForSplit){
        ArrayList<String> readyList = new ArrayList<String>();
            for (String splitWord : lineForSplit.split(" ")) {
                readyList.add(splitWord);
            }
        return readyList;
    }


    void fillData() {
        List<Alarm> listGetAlarms = db.getAllAlarms();//создание списка обьектов типа "запись" и заполнения его значениями всех записей взятых из БД

        for (Alarm cn : listGetAlarms) {//проходим про каждому обьекту списка
            alarms.add(new Alarm(cn.getID(), cn.get_hour(), cn.get_minute(), cn.get_delayHour(), cn.get_delayMinute(), cn.get_active(), cn.get_content(),
                    cn.get_Sound(), cn.get_repetDays()));
        }
    }







}

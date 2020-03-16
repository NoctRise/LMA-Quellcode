package com.example.student.lokalmobil.praktikum3;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MeinEchoClient extends AsyncTask<String, Void, Void> {
    private final String TAG = getClass().getSimpleName();
    String host = "192.168.2.102";
    Socket meinEchoSocket = null;
    boolean send;
    boolean stop;
    String messageToSend = "";

    @Override
    protected Void doInBackground(String... arg0) {

        try {

            meinEchoSocket = new Socket(host, 5556);
            OutputStream socketoutstr = meinEchoSocket.getOutputStream();
            OutputStreamWriter osr = new OutputStreamWriter(socketoutstr);
            BufferedWriter bw = new BufferedWriter(osr);

            InputStream socketinstr = meinEchoSocket.getInputStream();
            InputStreamReader isr = new InputStreamReader(socketinstr);
            BufferedReader br = new BufferedReader(isr);

            while (!stop) {
                if (send) {
                    send = false;
                    String antwort;

                    bw.write(messageToSend);
                    bw.newLine();
                    bw.flush();
                    antwort = br.readLine();

                    Log.d(TAG, "Host = " + host);
                    Log.d(TAG, "Echo = " + antwort);
                }
            }

            bw.close();
            br.close();
            meinEchoSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isSend() {
        return send;
    }

    public void setSend(boolean send) {
        this.send = send;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public void setMessageToSend(String messageToSend) {
        this.messageToSend = messageToSend;
    }


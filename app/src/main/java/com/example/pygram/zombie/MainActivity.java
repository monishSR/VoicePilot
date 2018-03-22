package com.example.pygram.zombie;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Set;


import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    protected static final int RESULT_SPEECH = 1;
    private ImageButton btnSpeak;
    private TextView txtSpeech;
    //normal movement commands
    private String[] commands = {"forward", "reverse", "backward", "stop"};
    //turn movement commands
    private String[] turnCmd = {"left", "right"};
    //Bluetooth adapter
    private BluetoothAdapter BA;
    private Map<String, Integer> dictionary;
    private static final UUID con_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ImageView DirectionImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        txtSpeech = (TextView) findViewById(R.id.txtSpeech);

        DirectionImage =(ImageView)findViewById(R.id.txtImage);

        dictionary = new HashMap<>();

        dictionary.put(commands[0], 0);
        dictionary.put(commands[1], 3);
        dictionary.put(commands[2], 3);
        dictionary.put(commands[3], 6);
        dictionary.put(turnCmd[0], 1);
        dictionary.put(turnCmd[1], 2);

        BA = BluetoothAdapter.getDefaultAdapter();

        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }

        btnSpeak.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);


                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-IN");

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                    txtSpeech.setText("");
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Oops! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });
    }

    public void onPause(){
        super.onPause();
        btConnect(6);
    }
    public void onDestroy(){
        super.onDestroy();
        btConnect(6);
        btConnect(7);
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    //Bluetooth connection validation method
    private synchronized void btConnect(int code){

        BluetoothSocket socket;
        String rpi_mac = "B8:27:EB:B1:28:54";
        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();
        if (pairedDevices.size() > 0) {

            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equals(rpi_mac)) {
                    try {
                        socket = device.createInsecureRfcommSocketToServiceRecord(con_UUID);
                        socket.connect();
                        DataOutputStream writeRpi = new DataOutputStream(socket.getOutputStream());
                        writeRpi.writeUTF(""+code+code+code+code);
                        writeRpi.close();
                        socket.close();
                    }
                    catch (IOException e1) {
                        Log.d("BT_TEST", "Error in connection");
                        e1.printStackTrace();
                        Toast.makeText(getApplicationContext(),"Error in connection",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txtSpeech.setText(findCommand(text.get(0)));
                }
                break;
            }

        }
    }


    private String findCommand(String cmd)
    {
        String[] splitCmd = cmd.split(" ");
        String str = "";
        int code = 0;
        boolean invCmd1 = false, invCmd2 = false;
        if(splitCmd.length>2) {
            btConnect(6);
            setImage(6);
            return ("Command not found");
        }
        for(String sCmd: commands) {
            if (sCmd.equals(splitCmd[0])) {
                str += sCmd;
                code += dictionary.get(sCmd);
                invCmd1 = true;
                break;
            }
        }
        for (String tCmd: turnCmd){
            if(tCmd.equals(splitCmd[0])){
                str += tCmd;
                invCmd2 = true;
                code += dictionary.get(tCmd);
                break;
            }
        }
        if(!(invCmd1 || invCmd2)){
            btConnect(6);
            setImage(6);
            return("Command not found");
        }
        if(splitCmd.length == 2){
            if(invCmd2 || str.equals("Stop")) {
                btConnect(6);
                setImage(6);
                return "Command not found!";
            }
            boolean invCmd = true;
            for(String tCmd: turnCmd)
            {
                if(tCmd.equals(splitCmd[1])) {
                    str += " " + tCmd;
                    code += dictionary.get(tCmd);
                    invCmd = false;
                    break;
                }
            }
            if(invCmd) {
                btConnect(6);
                setImage(6);
                return "Command not found";
            }
        }
        btConnect(code);
        setImage(code);
        return(str);
    }
    private void setImage(int code){
        switch (code){
            case 0: DirectionImage.setImageResource(R.drawable.forward);
                break;
            case 1: DirectionImage.setImageResource(R.drawable.front_left);
                break;
            case 2: DirectionImage.setImageResource(R.drawable.front_right);
                break;
            case 3: DirectionImage.setImageResource(R.drawable.backk);
                break;
            case 4: DirectionImage.setImageResource(R.drawable.back_left);
                break;
            case 5: DirectionImage.setImageResource(R.drawable.back_right);
                break;
            case 6: DirectionImage.setImageResource(R.drawable.stopp);
                break;
        }
    }
}

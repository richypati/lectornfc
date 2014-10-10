package com.helloworld.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.Charset;

public class HelloWorldNFCActivity extends Activity{

	private static final String TAG = HelloWorldNFCActivity.class.getName();

	protected NfcAdapter nfcAdapter;
	protected PendingIntent nfcPendingIntent;
    private WifiManager mWifiManager;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView data;
    private TextView wifiStatus;
    private TextView bluetoothStatus;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	   	Log.d(TAG, "\n + onCreate()");

        /* Inicializando componentes */
		setContentView(R.layout.main);
        data = (TextView) findViewById(R.id.data);
        wifiStatus = (TextView) findViewById(R.id.wifiStatus);
        bluetoothStatus = (TextView) findViewById(R.id.bluetoothStatus);

        /* Actualizando en los textView de la actividad principal
         * el estado del WiFi y del Bluetooth
         */
        updateWifiStatusText();
        updateBluetoothStatusText();

		// Inicializando el NFC
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Log.d(TAG, " + Inicializando NFC...");

        /* Se crea un PendingIntent (Intento pendiente) que será asociado a esta actividad
         * Cuando se detecte un intento, asignará los detalles del tag detectado y se lo asigna
         * a esta actividad.
         */
		nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        Log.d(TAG, " + Escuchando Intento...");
	}

    /**
     * Se realizan las acciones que se programen al detectar un intento. (Cuando se acerca un Tag
     * al dispositivo)
     * Se activa cuando se realiza un intento.
     * @param intent Intento capturado por el PendingIntent
     */
	@Override
	public void onNewIntent(Intent intent) {
        Log.d(TAG, "\n + onNewIntent() ");

        setIntent(intent);
        Log.d(TAG, " + Acción: " + intent.getAction());

        Tag tagDeIntento;

        /* Si la acción que describe el intento es ACTION_TAG_DISCOVERED, es decir,
         * si puso un tag cerca al dispositivo.
         */
	    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Log.d(TAG, " + ¡Un TAG fue escaneado!");

            // Se obtiene el Tag del intento detectado.
            tagDeIntento = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.d(TAG, "+ Obteniendo Tag...");

            // Se lee el contenido del Tag
            String prueba = readTag(tagDeIntento);

            //vibrar();


            /* Si en el TAG está escrito "WOFF" entonces se apaga el wifi,
             * Si está escrito "WON" entonces se enciende.
             */
            wifiStatus.setText("Espere...");
            bluetoothStatus.setText("Espere...");

            if (prueba.contains("WOFF")){
                changeWifiStatus(false);
            }
            if(prueba.contains("WON")){
                changeWifiStatus(true);
            }

            if (prueba.contains("BTOF")){
                changeBluetoothStatus(false);
            }
            if (prueba.contains("BTON")){
                changeBluetoothStatus(true);
            }

            printTagId(getIntent());
        }
	}

	protected void printTagId(Intent intent) {
		if(intent.hasExtra(NfcAdapter.EXTRA_ID)) {
			byte[] byteArrayExtra = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

            String tagId = toHexString(byteArrayExtra);

            TextView textView = (TextView) findViewById(R.id.tagID);
            textView.setText(tagId);
			Log.d(TAG, "Tag ID is: " + tagId);
		}
	}
	
	 /**
     * Convierte el array de bytes en un String de HEX
     *
     * @param buffer El buffer.
     * @return String Datos en Hexadecimal.
     */
    public String toHexString(byte[] buffer) {
		StringBuilder stringBuilder = new StringBuilder();
		for(byte b: buffer)
			stringBuilder.append(String.format("%02x ", b&0xff));
		return stringBuilder.toString().toUpperCase();
    }

    public void enableForegroundMode() {
        Log.d(TAG, "enableForegroundMode");

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED); // filter for all

        IntentFilter[] writeTagFilters = new IntentFilter[] {tagDetected};
        nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, writeTagFilters, null);
    }

    public void disableForegroundMode() {
        Log.d(TAG, "disableForegroundMode");

        nfcAdapter.disableForegroundDispatch(this);
    }

	@Override
	protected void onResume() {
		super.onResume();

        Log.d(TAG, "onResume");

		enableForegroundMode();
	}

	@Override
	protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause");

		disableForegroundMode();
	}

	private void vibrar() {
		Log.d(TAG, " + Vibrando...");
		
		Vibrator vibrador = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE) ;
		vibrador.vibrate(500);
	}

    /**
     * Escribe datos en el Tag
     * @param tag Tag que se obtuvo del intento
     * @param tagData Datos que se van a almacenar en el Tag
     */
    public void writeTag(Tag tag, String tagData) {
        Log.d(TAG, " + Escribiendo en Tag...");

        /* Se obtiene un objento tipo MifareUltralight (Tipo de tecnología de Tag que vamos a leer)
         * dando el tag.
          */
        MifareUltralight mifare = MifareUltralight.get(tag);

        while (tagData.length()<=48){
            tagData += ".";
        }

        try {
            // Se habilita la Lectura/Escritura del Tag
            mifare.connect();

            /* - Escribiendo en el Tag...
             * - Se escribe página por página.
             * - Como cada página es de 4 bytes entonces se debe leer del String tagData
             *   cuatro caracteres por página y luego convertirlos a bytes.
             */
            int pagina=4;
            int caracter=0;
            while (pagina<16){
                mifare.writePage(pagina, tagData.substring(caracter, caracter + 4).getBytes(Charset.forName("US-ASCII")));
                pagina++;
                caracter += 4;
            }

        } catch (IOException e) {
            Log.e(TAG, " + IOException mientras se escribía en MifareUltralight Tag...", e);
        } finally {
            try {
                // Deshabilita la Lectura/Escritura del Tag
                mifare.close();
            } catch (IOException e) {
                Log.e(TAG, " + IOException mientras se cerraba MifareUltralight...", e);
            }
        }
    }

    /**
     * Lee los datos que se encuentran almacenados en el Tag
     * (Lee de los bloques 4 a 15 porque los primeros 4 (0-3) están reservados)
     * @param tag Tag que se obtuvo del intento
     * @return String Datos concatenados de la página 4 a la 15 del Tag
     */
    public String readTag(Tag tag) {
        Log.d(TAG, " + Leyendo Tag...");

        /* Se obtiene un objento tipo MifareUltralight (Tipo de tecnología de Tag que vamos a leer)
         * dando el tag.
          */
        MifareUltralight mifare = MifareUltralight.get(tag);

        try {
            // Se habilita la Lectura/Escritura del Tag
            mifare.connect();

            /* Se leen los datos desde la página cuatro hasta la 16
             * Cada readPages(int) lee 4 páginas desde la página indicada, es decir, lee 16 bytes.
             */
            byte[] payload1 = mifare.readPages(4);
            byte[] payload2 = mifare.readPages(8);
            byte[] payload3 = mifare.readPages(12);

            // Se hace una conversión de los datos leídos en bytes a US-ASCII y se concatenan.
            String data= new String(payload1, Charset.forName("US-ASCII"));
            data += new String(payload2, Charset.forName("US-ASCII"));
            data += new String(payload3, Charset.forName("US-ASCII"));

            Log.d(TAG, " + Datos en Tag: " + data);

            return data;
        } catch (IOException e) {
            Log.e(TAG, "+ IOException mientras se leían datos de MifareUltralight Tag...", e);
        } finally {
            if (mifare != null) {
                try {
                    // Deshabilita la Lectura/Escritura del Tag
                    mifare.close();
                }
                catch (IOException e) {
                    Log.e(TAG, " + Error cerrando tag...", e);
                }
            }
        }
        return null;
    }

    private void updateWifiStatusText(){
        mWifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);

        if (mWifiManager.isWifiEnabled()){

            wifiStatus.setText("ON");
        }else{
            wifiStatus.setText("OFF");
        }
    }

    private void updateBluetoothStatusText(){

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (null == mBluetoothAdapter)
        {
            Log.e(TAG, " + Error con Bluetooth Adapter.");
            return;
        }

        if (mBluetoothAdapter.isEnabled()){
            bluetoothStatus.setText("ON");
            Log.d(TAG, " + Habilitando Bluetooth");
        }else{
            bluetoothStatus.setText("OFF");
            Log.d(TAG, " + Deshabilitando Bluetooth");
        }
    }

    private void changeWifiStatus(boolean turnOnWifi){
        mWifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);

        if (mWifiManager.isWifiEnabled() && !turnOnWifi){
            mWifiManager.setWifiEnabled(false);// Deshabilitando WiFi
            Log.d(TAG, " + Habilitando Wifi...");
        }else{
            if (!mWifiManager.isWifiEnabled() && turnOnWifi) {
                mWifiManager.setWifiEnabled(true);// Habilitando WiFi
                Log.d(TAG, " + Deshabilitando Wifi")
                while (!mWifiManager.isWifiEnabled()) ;
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, " + Error al esperar 1 segundo esperando estado Wifi.");
            e.printStackTrace();
        }
        updateWifiStatusText();
    }

    private void changeBluetoothStatus(boolean turnOnBT){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled() && !turnOnBT) {
            mBluetoothAdapter.disable();
        }else{
            if (!mBluetoothAdapter.isEnabled() && turnOnBT){
                mBluetoothAdapter.enable();
                while (!mBluetoothAdapter.isEnabled()) ;
            }
        }
        try{
            Thread.sleep(1000);
        }catch (InterruptedException e) {
            Log.e(TAG, "+ Error al esperar 1 segundo esperando estado Bluetooth.");
            e.printStackTrace();
        }
        updateBluetoothStatusText();
    }
}
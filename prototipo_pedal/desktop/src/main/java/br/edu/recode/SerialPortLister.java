package br.edu.recode;

import com.fazecast.jSerialComm.SerialPort;

/** Lists serial ports before the ESP32 is connected to the game. */
public final class SerialPortLister {
    private SerialPortLister() { }

    public static void main(String[] args) {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.out.println("Nenhuma porta serial encontrada. Conecte o ESP32 com um cabo USB de dados.");
            return;
        }
        for (SerialPort port : ports) {
            System.out.println(port.getSystemPortName() + "\t" + port.getDescriptivePortName());
        }
    }
}

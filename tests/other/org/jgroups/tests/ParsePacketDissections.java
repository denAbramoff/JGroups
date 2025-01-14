package org.jgroups.tests;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jgroups.util.Util;

/**
 * If you use `tshark ... -w data.tshark > parsed.txt` when the test is over, the parse will continue running and can take a lot of time.
 * In order to speed up, we are going to parse the output from `-w `
 * Open in Wireshark the output resulted from -w from the tshark command.
 *
 *    Columns: No. Time Source Destination Protocol length Info
 *
 *    Menu View -> Time Display Format -> Time of the Day
 *    Menu View -> Time Display Format -> Automatic ( from capture file )
 *       The hour in the Wireshark can be different when comparing with the the application log because of the timezone.
 *
 *    If you know the time of the error, you can apply a display filter to reduce the exported file size.
 *    Example: (frame.time >= "Jul 15, 2022 15:47:00" && frame.time <= "Jul 15, 2022 15:49:00")
 *
 *    Menu File -> Export Packet Dissections -> As Plain Text
 *       Fill the File name:
 *       All packets captured
 *       Packet Format: ( keep selected only the folling )
 *          Summary line
 *          Bytes
 *    In the bottom, a status bar with the progress will appear.
 */
public class ParsePacketDissections {

   public static void main(String[] args) throws Exception {
      boolean print_vers=false, binary_to_ascii=true, parse_discovery_responses=true, tcp=false;
      ParseMessages.InnerParseMessages inner = new ParseMessages.InnerParseMessages(print_vers, binary_to_ascii, parse_discovery_responses, null);
      System.out.println("No.,Time,Source,Destination,Protocol,Length,Info");
      Long count = null;
      try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
         String line;
         while ((line = br.readLine()) != null) {
            // a display field can be applied
            if (count == null) {
               count = Long.valueOf(line.trim().split(" ")[0]);
            }
            // begin of a packet
            if (line.trim().length() > 0 && line.trim().startsWith(count++ + "")) {
               List<String> innerData = Arrays.stream(line.trim().split(" ")).filter(s -> s.length() > 0).collect(Collectors.toList());
               System.out.println(String.format("---> %s,%s,%s,%s,%s,%s,%s", innerData.remove(0), innerData.remove(0), innerData.remove(0), innerData.remove(0), innerData.remove(0), innerData.remove(0), innerData.toString().replace("[", "").replace("]", "").replace(", ", " ")));
            } else {
               // line will be empty
               // br.readLine will return the next line
               // when the line is empty, it is the end of the packet
               StringBuilder sb = new StringBuilder();
               while ((line = br.readLine()) != null) {
                  if (line.trim().length() == 0) {
                     break;
                  }
                  // the output has a pattern
                  sb.append(line.substring(4, 54).replaceAll(" ", ""));
               }
               String jgroupsMessage = sb.substring(32 + 32 + 20);
               Util.parse(inner.createInputStream(new ByteArrayInputStream(jgroupsMessage.getBytes())), inner.msg_consumer, inner.batch_consumer, tcp);
            }
         }
      }
   }
}

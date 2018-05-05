package dataProcessors;

import Algorithm.AlgorithmType;
import Algorithm.ClusteringAlgorithm;
import Algorithm.Configuration;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.chart.XYChart;
import ui.AppUI;
import vilij.templates.ApplicationTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

public class ClusteringProcessor extends Thread implements DataProcessor {
    ApplicationTemplate applicationTemplate;

    private ClusteringAlgorithm clusteringAlgorithm;
    private Configuration configuration;
    private int currentIteration;
    private boolean running;
    private XYChart<Number, Number> chart;
    private Data data;
    public ClusteringProcessor(AlgorithmType clusteringAlgorithm, XYChart<Number,Number> chart,
                               Data data, ApplicationTemplate applicationTemplate){
        this.clusteringAlgorithm=(ClusteringAlgorithm)clusteringAlgorithm;
        this.configuration=clusteringAlgorithm.getConfiguration();
        this.chart=chart;
        this.applicationTemplate=applicationTemplate;
        this.data=data;
        running=true;
        currentIteration=0;
    }
    @Override
    public void run(){
        update();
        ((AppUI)applicationTemplate.getUIComponent()).disableScrnShotButton(false);
    }

    @Override
    public void update(){
        try {
            Method getOutputMethod = null;
            for (Method m : clusteringAlgorithm.getClass().getMethods()) {
                if (m.getName().equals("getOutput"))
                     getOutputMethod = m;
            }
            if (configuration.continous && getOutputMethod!=null) {
                for (int i = 1; i <= configuration.MaxInterval; i++) {
                    clusteringAlgorithm.run();
                    if (i % configuration.IterationInterval == 0) {
                        currentIteration = i;
                        System.out.println(data.getDataLabels());
                        System.out.println(data.getDataPoints());
                        data = (Data) getOutputMethod.invoke(clusteringAlgorithm);
                        display();
                    }
                    try {
                        sleep(2000);
                        if(!running) {
                            Platform.runLater(()-> ((AppUI)applicationTemplate.getUIComponent()).clearChart());
                            break;
                        }
                    } catch (InterruptedException e) {
                        //Do Nothing
                    }
                }
            } else if(getOutputMethod!=null) {

                //do something
            }
        }catch (IllegalAccessException | InvocationTargetException e){
            //Method Not Found
            //Do Nothing
        }
    }

    private void display(){
        AppUI ui= (AppUI)applicationTemplate.getUIComponent();
        Platform.runLater(()->{
            chart.setAnimated(false);
            ui.clearChart();
            changeLabel();
            toChartData(data,chart);
            chart.setAnimated(true);
        });
    }
    private void changeLabel(){
        HashMap<String, String> dataLabels = data.getDataLabels();
        HashSet<String> labels =new HashSet<>();
        for (String entry: dataLabels.keySet()) {
            labels.add(dataLabels.get(entry));
        }
        data.setLabel(labels);
    }

    @Override
    public boolean CheckState() {
        return isAlive();
    }

    @Override
    public void terminate(){
        running=false;
    }
}

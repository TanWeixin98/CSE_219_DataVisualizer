package dataProcessors;


import Algorithm.ClassificationAlgorithm;
import Algorithm.Configuration;
import Algorithm.RandomClassification;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.chart.XYChart;
import settings.AppPropertyTypes;
import ui.AppUI;
import vilij.propertymanager.PropertyManager;
import vilij.templates.ApplicationTemplate;

import java.util.List;

public class ClassificationProcessor extends Thread implements DataProcessor{
    private ClassificationAlgorithm classificationAlgorithm;
    private Configuration configuration;
    private ApplicationTemplate applicationTemplate;
    private int currentIteration;
    private XYChart<Number, Number> chart;
    private int chartOriginalDataSize;
    private double MaxX, MinX;
    private boolean running;
    public ClassificationProcessor(ClassificationAlgorithm classificationAlgorithm,
                                   XYChart<Number,Number> chart,
                                   ApplicationTemplate applicationTemplate,
                                   double MaxX, double MinX){
        this.classificationAlgorithm=classificationAlgorithm;
        this.configuration=classificationAlgorithm.getConfiguration();
        this.applicationTemplate=applicationTemplate;
        this.chart=chart;
        running=true;
        this.MaxX= MaxX;
        this.MinX= MinX;
        if(MaxX==MinX){
            this.MinX=MinX-10;
            this.MaxX=MaxX+10;
        }
        currentIteration=1;
    }

    public void setChartOriginalDataSize(){
        chartOriginalDataSize=chart.getData().size();
    }
    @Override
    public void run() {
        update();
        ((AppUI)applicationTemplate.getUIComponent()).disableScrnShotButton(false);
    }

    @Override
    public void update() {
        if(configuration.continous){
            for(int i=1;i<=configuration.MaxInterval;i++){
                ((RandomClassification)classificationAlgorithm).run();
                if(i%configuration.IterationInterval==0) {
                    currentIteration=i;
                    List<Integer> list=classificationAlgorithm.getOutput();
                    while(list.get(1) ==0){
                        ((RandomClassification)classificationAlgorithm).run();
                        list=classificationAlgorithm.getOutput();
                    }
                    display(getLinePoints(list));
                    try {
                        sleep(2000);
                        if(!running)
                            break;
                    } catch (InterruptedException e) {
                        //do nothing2
                    }
                }
            }
        }else{
            for(int i=1;i<=configuration.MaxInterval;i++){
                ((RandomClassification)classificationAlgorithm).run();
                if(i%configuration.IterationInterval==0) {
                    currentIteration=i;
                    List<Integer> list=classificationAlgorithm.getOutput();
                    while(list.get(1) ==0){
                        ((RandomClassification)classificationAlgorithm).run();
                        list=classificationAlgorithm.getOutput();
                    }
                    display(getLinePoints(list));
                    try {
                        synchronized (this) {
                            wait();
                        }
                    } catch (InterruptedException e) {
                        //If button is click it will resume
                        if(!running)
                            break;
                    }
                }
            }
        }
    }
    private Point2D[] getLinePoints(List<Integer> list){
        Point2D[] point2DS = new Point2D[2];
        point2DS[0]=new Point2D(MinX,(MinX*list.get(0)+list.get(2)/(-1*list.get(1))));
        point2DS[1]=new Point2D(MaxX,(MaxX*list.get(0)+list.get(2)/(-1*list.get(1))));
        return point2DS;
    }
    private void display(Point2D[] point2DS){
        AppUI ui= (AppUI)applicationTemplate.getUIComponent();
        XYChart.Series<Number,Number> series = new XYChart.Series<>();

        Platform.runLater(()->{
            chart.setAnimated(true);
            if(chartOriginalDataSize!=chart.getData().size()) {
                chart.setAnimated(false);
                chart.getData().remove(chart.getData().size() - 1);
            }
            series.getData().add(new XYChart.Data<>(point2DS[0].getX(),point2DS[0].getY()));
            series.getData().add(new XYChart.Data<>(point2DS[1].getX(),point2DS[1].getY()));
            chart.getData().add(series);
            series.setName(PropertyManager.getManager().getPropertyValue(AppPropertyTypes.RANDOM_CLASSIFICATION.name()));
            ui.showDisplayPane(configuration.continous);
            ui.setRunInfo(currentIteration,configuration.MaxInterval);
            ui.disableScrnShotButton(configuration.continous);
            for (XYChart.Data<Number,Number> data: series.getData()) {
                data.getNode().setVisible(false);
            }

        });
    }

    public void terminate(){
        running=false;
    }
}

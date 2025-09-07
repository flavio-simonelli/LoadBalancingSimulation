package it.pmcsn.lbsim.models.domain.scaling.horizontalscaler;

public class NoneHorizontalScaler implements HorizontalScaler {

    @Override
    public void setLastActionAt(double time) {
    }

    @Override
    public Action notifyJobDeparture(double responseTimeSeconds, double nowSeconds) {
        return Action.NONE;
    }

    @Override
    public int getScaleActions() {
        return 0;
    }
}

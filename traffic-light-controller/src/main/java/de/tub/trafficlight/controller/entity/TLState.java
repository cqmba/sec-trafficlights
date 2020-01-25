package de.tub.trafficlight.controller.entity;

public enum TLState {

    //State Machine changed regarding requirements!
    S0_MG_SR_PM {
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            if (isEmergencyMain){
                return S0_MG_SR_PM;
            } else if (isEmergencySide){
                return STOPS;
            }
            return S1_MY_SR_PR;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.GREEN;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.GREEN;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 25000;
        }
    },
    S1_MY_SR_PR{
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            if (isEmergencyMain){
                //different than requirements but makes more sense
                return S5_MRY_SR_PM;
            } else if (isEmergencySide){
                //different than requirements but makes more sense
                return S2_MR_SRY_PS;
            }
            return S2_MR_SRY_PS;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.YELLOW;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 5000;
        }
    },
    S2_MR_SRY_PS {
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            if (isEmergencyMain){
                return STOPM;
            } else if (isEmergencySide){
                return S3_MR_SG_PS;
            }
            return S3_MR_SG_PS;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.YELLOWRED;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.GREEN;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 5000;
        }
    },
    S3_MR_SG_PS {
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            if (isEmergencyMain){
                return STOPM;
            } else if (isEmergencySide){
                return S3_MR_SG_PS;
            }
            return S4_MR_SY_PR;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.GREEN;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.GREEN;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 25000;
        }
    },
    S4_MR_SY_PR {
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            if (isEmergencyMain){
                //different
                return S5_MRY_SR_PM;
            } else if (isEmergencySide){
                //different
                return S2_MR_SRY_PS;
            }
            return S5_MRY_SR_PM;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.YELLOW;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 5000;
        }
    },
    S5_MRY_SR_PM {
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            if (isEmergencyMain){
                return S0_MG_SR_PM;
            } else if (isEmergencySide){
                return STOPS;
            }
            return S0_MG_SR_PM;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.YELLOWRED;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.GREEN;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 5000;
        }
    },
    STOPM {
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            return S0_MG_SR_PM;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.YELLOW;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 5000;
        }
    },
    STOPS {
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            return S3_MR_SG_PS;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.YELLOW;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.RED;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 5000;
        }
    },
    EMERGENCY {
        @Override
        public TLState nextState(boolean isEmergencyMain, boolean isEmergencySide) {
            return S0_MG_SR_PM;
        }

        @Override
        public TLColor getCurrentMainRoadColor() {
            return TLColor.YELLOWBLINKING;
        }

        @Override
        public TLColor getCurrentSideRoadColor() {
            return TLColor.YELLOWBLINKING;
        }

        @Override
        public TLColor getCurrentMainPedestrianColor() {
            return TLColor.YELLOWBLINKING;
        }

        @Override
        public TLColor getCurrentSidePedestrianColor() {
            return TLColor.YELLOWBLINKING;
        }

        @Override
        public int getCurrentStandardTransitionTimeMs() {
            return 60000;
        }
    };

    public abstract TLState nextState(boolean isEmergencyMain, boolean isEmergencySide);
    public abstract TLColor getCurrentMainRoadColor();
    public abstract TLColor getCurrentSideRoadColor();
    public abstract TLColor getCurrentMainPedestrianColor();
    public abstract TLColor getCurrentSidePedestrianColor();
    public abstract int getCurrentStandardTransitionTimeMs();
}

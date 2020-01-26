package de.tub.trafficlight.controller.entity;

public enum TLPosition {
    MAIN_ROAD_WEST {
        @Override
        public boolean isMain() {
            return true;
        }

        @Override
        public boolean isSide() {
            return false;
        }
    },
    MAIN_ROAD_EAST {
        @Override
        public boolean isMain() {
            return true;
        }

        @Override
        public boolean isSide() {
            return false;
        }
    },
    SIDE_ROAD_NORTH {
        @Override
        public boolean isMain() {
            return false;
        }

        @Override
        public boolean isSide() {
            return true;
        }
    },
    SIDE_ROAD_SOUTH {
        @Override
        public boolean isMain() {
            return false;
        }

        @Override
        public boolean isSide() {
            return true;
        }
    },
    UNSPECIFIED {
        @Override
        public boolean isMain() {
            return false;
        }

        @Override
        public boolean isSide() {
            return false;
        }
    };

    public abstract boolean isMain();
    public abstract boolean isSide();
}

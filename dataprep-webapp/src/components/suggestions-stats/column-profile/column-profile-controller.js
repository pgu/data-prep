(function() {
    'use strict';

    function ColumnProfileCtrl($scope, StatisticsService, PlaygroundService, PreparationService, RecipeService) {
        var vm = this;
        vm.statisticsService = StatisticsService;
        vm.chartConfig = {};

        vm.barchartClickFn = function barchartClickFn (item){
            return StatisticsService.addFilter(item.data);
        };
        
        //------------------------------------------------------------------------------------------------------
        //----------------------------------------------AGGREGATION---------------------------------------------
        //------------------------------------------------------------------------------------------------------
        /**
         * @ngdoc property
         * @name aggregations
         * @propertyOf data-prep.actions-suggestions-stats.controller:ColumnProfileCtrl
         * @description The list of possible aggregations
         * @type {array}
         */
        vm.aggregations =  [
            {id: 'sum', name: 'SUM'},
            {id: 'max', name: 'MAX'},
            {id: 'min', name: 'MIN'},
            {id: 'count', name: 'COUNT'},
            {id: 'average', name: 'AVERAGE'},
            {id: 'median', name: 'MEDIAN'}
        ];

        /**
         * @ngdoc method
         * @name getCurrentAggregation
         * @propertyOf data-prep.actions-suggestions-stats.controller:ColumnProfileCtrl
         * @description The current aggregations
         * @return {string} The current aggregation name
         */
        vm.getCurrentAggregation = function getCurrentAggregation() {
            return StatisticsService.histogram && StatisticsService.histogram.aggregation ?
                StatisticsService.histogram.aggregation.name:
                'LINE_COUNT';
        };

        /**
         * @ngdoc method
         * @name changeAggregation
         * @methodOf data-prep.actions-suggestions-stats.controller:ColumnProfileCtrl
         * @param {object} column The column to aggregate
         * @param {object} aggregation The aggregation to perform
         * @description Trigger a new aggregation graph
         */
        vm.changeAggregation = function changeAggregation(column, aggregation) {
            if(StatisticsService.histogram &&
                StatisticsService.histogram.aggregationColumn === column &&
                StatisticsService.histogram.aggregation === aggregation) {
                return;
            }

            var datasetId = PlaygroundService.currentMetadata.id;
            var preparationId = PreparationService.currentPreparationId;
            var stepId = preparationId ? RecipeService.getLastActiveStep().id : null;

            StatisticsService.processAggregation(datasetId, preparationId, stepId, column, aggregation);
        };

        //------------------------------------------------------------------------------------------------------
        //----------------------------------------------GEO CHARTS ---------------------------------------------
        //------------------------------------------------------------------------------------------------------
        /**
         * Common highcharts options
         * @param clickFn - the click callback
         * @returns {{exporting: {enabled: boolean}, legend: {enabled: boolean}}}
         */
        var initCommonChartOptions = function(clickFn) {
            return {
                credits: {
                    enabled: false
                },
                exporting: {
                    enabled: false
                },
                legend: {
                    enabled: false
                },
                plotOptions: {
                    series: {
                        cursor: 'pointer',
                        point: {
                            events: {
                                click: clickFn
                            }
                        }
                    }
                }
            };
        };

        /**
         * Geo specific highcharts options
         * @param clickFn - the click callback
         * @param min - min value (defined for color)
         * @param max - max value (defined for color)
         * @returns {{exporting, legend}|{exporting: {enabled: boolean}, legend: {enabled: boolean}}}
         */
        var initGeoChartOptions = function(clickFn, min, max) {
            var options = initCommonChartOptions(clickFn);

            options.tooltip = {
                enabled: true,
                headerFormat: '',
                pointFormat: '{point.name}: <b>{point.value}</b>'
            };
            options.colorAxis = {
                min: min,
                max: max
            };
            options.mapNavigation = {
                enabled: true,
                buttonOptions: {
                    verticalAlign: 'bottom'
                }
            };

            return options;
        };

        /**
         * Init a geo distribution chart
         * @param column
         */
        var buildGeoDistribution = function(column) {
            var geoChartAction = function() {
                StatisticsService.addFilter(this['hc-key'].substring(3));
                console.log('State: '  + this['hc-key'] + ', value: ' + this.value);
            };

            vm.stateDistribution = StatisticsService.getGeoDistribution(column);

            var data = vm.stateDistribution.data;
            var min = data[data.length - 1].value;
            var max = data[0].value;

            vm.chartConfig = {
                options: initGeoChartOptions(geoChartAction, min, max),
                chartType: 'map',
                title: {text: column.name + ' distribution'},
                series: [
                    {
                        id: column.id,
                        data: data,
                        mapData: Highcharts.maps[vm.stateDistribution.map],
                        joinBy: 'hc-key',
                        states: {
                            hover: {
                                color: '#BADA55'
                            }
                        }
                    }
                ]
            };
        };

        //------------------------------------------------------------------------------------------------------
        //-------------------------------------------------WATCHERS---------------------------------------------
        //------------------------------------------------------------------------------------------------------
        /**
         * Init chart on column selection change
         */
        $scope.$watch(
            function() {
                return StatisticsService.stateDistribution;
            },
            function(column) {
                vm.stateDistribution = null;
                if(column) {
                    buildGeoDistribution(column);
                }
            }
        );

    }

    /**
     * @ngdoc property
     * @name processedData
     * @propertyOf data-prep.actions-suggestions-stats.controller:ColumnProfileCtrl
     * @description The data to display
     * This is bound to {@link data-prep.statistics:StatisticsService StatisticsService}.histogram
     */
    Object.defineProperty(ColumnProfileCtrl.prototype,
        'histogram', {
            enumerable: true,
            configurable: false,
            get: function () {
                return this.statisticsService.histogram;
            }
        });

    /**
     * @ngdoc property
     * @name aggregationColumns
     * @propertyOf data-prep.actions-suggestions-stats.controller:ColumnProfileCtrl
     * @description The numeric columns list of the dataset.
     * This is bound to {@link data-prep.statistics:StatisticsService StatisticsService}.getAggregationColumns()
     */
    Object.defineProperty(ColumnProfileCtrl.prototype,
        'aggregationColumns', {
            enumerable: true,
            configurable: true,
            get: function () {
                return this.statisticsService.getAggregationColumns();
            }
        });

    angular.module('data-prep.column-profile')
        .controller('ColumnProfileCtrl', ColumnProfileCtrl);
})();
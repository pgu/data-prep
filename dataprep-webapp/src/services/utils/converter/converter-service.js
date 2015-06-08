(function() {
    'use strict';

    /**
     * @ngdoc service
     * @name data-prep.services.utils.service:ConverterService
     * @description Converter service. This service help to convert data
     */
    function ConverterService() {

        /**
         * @ngdoc method
         * @name toInputType
         * @methodOf data-prep.services.utils.service:ConverterService
         * @param {string} type - the type to convert
         * @description Convert backend type to HTML input type
         * @returns {string} - the converted type
         */
        this.toInputType = function(type) {
            switch (type) {
                case 'numeric':
                case 'integer':
                case 'double':
                case 'float':
                    return 'number';
                case 'boolean':
                    return 'checkbox';
                default:
                    return 'text';
            }
        };

        /**
         * @ngdoc method
         * @name simplifyType
         * @methodOf data-prep.services.utils.service:ConverterService
         * @param {string} type - the type to convert
         * @description Convert backend type to a simplified, more user friendly, one
         * @returns {string} - the simplified type
         */
        this.simplifyType = function(type) {
               switch (type) {
                   case 'numeric':
                   case 'integer':
                   case 'double':
                   case 'float':
                       return 'number';
                   case 'boolean':
                       return 'boolean';
                   case 'string':
                   case 'char':
                        return 'text';
                   case 'date':
                        return 'date';
                   default:
                       return 'text';
               }
        };

    }

    angular.module('data-prep.services.utils')
        .service('ConverterService', ConverterService);

})();
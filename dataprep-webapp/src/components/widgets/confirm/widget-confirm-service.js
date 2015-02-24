(function() {
    'use strict';

    function TalendConfirmService($rootScope, $compile, $document, $q, $timeout) {
        var body = $document.find('body').eq(0);
        var self = this;

        /**
         * Create confirm modal isolated scope
         * @param text - the text to display
         */
        var createScope = function(texts) {
            if(self.modalScope) {
                throw new Error('A confirm popup is already created');
            }
            self.modalScope = $rootScope.$new(true);
            self.modalScope.texts = texts;
        };

        /**
         * Destroy the modal scope
         */
        var removeScope = function() {
            self.modalScope.$destroy();
            self.modalScope = null;
        };

        /**
         * Create the confirm modal element and attach it to the body
         */
        var createElement = function() {
            self.element = angular.element('<talend-confirm></talend-confirm>');
            $compile(self.element)(self.modalScope);
            body.append(self.element);
        };

        /**
         * Remove the the element
         */
        var removeElement = function() {
            self.element.remove();
            self.element = null;
        };

        /**
         * Remove the modal and reset everything
         */
        var close = function() {
            removeScope();
            removeElement();

            self.confirmResolve = null;
            self.confirmReject = null;
        };

        /**
         * Resolve the modal promise and destroy the modal
         */
        this.resolve = function() {
            self.confirmResolve();
            $timeout(close);
        };

        /**
         * Reject the modal promise and destroy the modal
         * @param cause - 'dismiss' if the modal is closed without clicking on a button
         */
        this.reject = function(cause) {
            self.confirmReject(cause);
            $timeout(close);
        };

        /**
         * Create the confirm modal element and return a promise that will be resolve on button click or modal dismiss
         * @param text - the text to display
         * @returns Promise
         */
        this.confirm = function() {
            var texts = Array.prototype.slice.call(arguments);
            createScope(texts);
            createElement();

            return $q(function(resolve, reject) {
                self.confirmResolve = resolve;
                self.confirmReject = reject;
            });
        };
    }

    angular.module('talend.widget')
        .service('TalendConfirmService', TalendConfirmService);
})();
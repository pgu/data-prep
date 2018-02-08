/*  ============================================================================

 Copyright (C) 2006-2018 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

const ACCEPTED_STATUS = 202;
const LOOP_DELAY = 1000;
const RUNNING_STATUS = 'RUNNING';
const ALLOWED_METHODS = ['GET', 'HEAD'];

/**
 * @ngdoc service
 * @name data-prep.services.rest.service:RestQueuedMessageHandler
 * @description Queued message interceptor
 */
export default function RestQueuedMessageHandler($q, $injector, $timeout, RestURLs) {
	'ngInject';

	function checkStatus(url) {
		const $http = $injector.get('$http');

		return new Promise((resolve, reject) => {
			$http.get(url)
				.then(({ data }) => (data.status === RUNNING_STATUS ? reject : resolve)(data));
		});
	}

	function loop(url) {
		function checker(url) {
			return checkStatus(url)
				.catch(() => $timeout(LOOP_DELAY).then(() => checker(url)));
		}
		return checker(url);
	}

	return {
		/**
		 * @ngdoc method
		 * @name response
		 * @methodOf data-prep.services.rest.service:RestQueuedMessageHandler
		 * @param {object} response - the intercepted response
		 * @description If a 202 occurs, loop until the status change from RUNNING to anything else
		 */
		response(response) {
			const { headers, config, status } = response;

			if (status === ACCEPTED_STATUS && ALLOWED_METHODS.includes(config.method)) {
				return loop(`${RestURLs.serverUrl}${headers('Location')}`)
					.then((data) => {
						const $http = $injector.get('$http');
						return $http({
							method: config.method,
							url: `${RestURLs.serverUrl}${data.result.downloadUrl}`,
						});
					});
			}

			return $q.resolve(response);
		},
	};
}

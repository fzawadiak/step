/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
angular.module('gridControllers', [ 'dataTable', 'step' ])

.controller('GridCtrl', ['$scope', 'stateStorage',
    function($scope, $stateStorage) {
      $stateStorage.push($scope, 'grid');
      
      $scope.autorefresh = true;

      if($scope.$state == null) { $scope.$state = 'tokens' };
      
      $scope.tabs = [
          { id: 'agents'},
          { id: 'tokens'},
          { id: 'adapters'},
          { id: 'quotamanager'}
      ]
      
      $scope.activeTab = function() {
        return _.findIndex($scope.tabs,function(tab){return tab.id==$scope.$state});
      }
      
      $scope.onSelection = function(tabid) {
        return $scope.$state=tabid;
      }
}])   

.controller('AgentListCtrl', [
    '$scope',
    '$interval',
    '$http',
    'helpers',
    function($scope, $interval, $http, helpers) {
      $scope.$state = 'agents';
      
      $scope.datatable = {}
      
      $scope.loadTable = function loadTable() {
        $http.get("rest/grid/agent").then(
          function(response) {
            var data = response.data;
            var dataSet = [];
            for (i = 0; i < data.length; i++) {
              dataSet[i] = [ data[i].agentId, data[i].agentUrl];
            }
            $scope.tabledef.data = dataSet;
          });
        };

        $scope.tabledef = {};
        $scope.tabledef.columns = [ { "title" : "ID", "visible" : false}, { "title" : "Url" } ];

//        $scope.tabledef.actions = [{"label":"Interrupt","action":function() {$scope.interruptSelected()}},
//                                   {"label":"Resume","action":function() {$scope.resumeSelected()}}];
//        
        $scope.loadTable();
        
        $scope.interruptSelected = function() {
          var rows = $scope.datatable.getSelection().selectedItems;
          
          for(i=0;i<rows.length;i++) {
            $scope.interrupt(rows[i][0]);       
          }
        };
          
        $scope.interrupt = function(id) {
          $http.put("rest/grid/agent/"+id+"/interrupt").then(function() {
                $scope.loadTable();
            });
        }
        
        $scope.resumeSelected = function() {
          var rows = $scope.datatable.getSelection().selectedItems;
          
          for(i=0;i<rows.length;i++) {
            $scope.resume(rows[i][0]);       
          }
        };
          
        $scope.resume = function(id) {
          $http.put("rest/grid/agent/"+id+"/resume").then(function() {
                $scope.loadTable();
            });
        }
          
        var refreshTimer = $interval(function(){
            if($scope.autorefresh){$scope.loadTable()}}, 2000);
          
          $scope.$on('$destroy', function() {
            $interval.cancel(refreshTimer);
          });

      } ])

.controller('AdapterListCtrl', [
  	'$scope',
  	'$compile',
  	'$interval',
  	'$http',
  	'helpers',
  	function($scope, $compile, $interval, $http, helpers) {
  	  $scope.$state = 'adapters';
  	  
  	  $scope.keySelectioModel = {};
  	  
  	  $scope.table = {};
  	  
  	  $http.get("rest/grid/keys").then(
          function(response) { 
            $scope.keys = ['url']; $scope.keySelectioModel['url']=true;
            _.each(response.data,function(key){$scope.keys.push(key); $scope.keySelectioModel[key]=false});
          })
      
  	  $scope.loadTable = function loadTable() {
  	    var queryParam='';
  	    _.each(_.keys($scope.keySelectioModel),function(key){
  	      if($scope.keySelectioModel[key]) {
  	      queryParam+='groupby='+key+'&'
  	      }
  	    })
  		$http.get("rest/grid/token/usage?"+queryParam).then(
  			function(response) {
  			  var data = response.data;
  			  var dataSet = [];
  			  for (i = 0; i < data.length; i++) {
  				dataSet[i] = [ helpers.formatAsKeyValueList(data[i].key), {usage:data[i].usage, capacity:data[i].capacity, error:data[i].error} ];
  			  }
  			  $scope.tabledef.data = dataSet;
  			});
  	  };
  	  
  	  $scope.$watchCollection('keySelectioModel',function() {$scope.loadTable()});
  	  
  	  var refreshTimer = $interval(function(){
        if($scope.autorefresh){$scope.loadTable();}}, 2000);
      
      $scope.$on('$destroy', function() {
        $interval.cancel(refreshTimer);
      });
  	  
  	  $scope.tabledef = {};
  	  $scope.tabledef.columns = [ 
  	    {
          "title" : "URL"
        }, 
        {
          "title" : "Usage",
          "createdCell" : function(td, cellData, rowData, row, col) {
            var rowScope = $scope.$new(true, $scope);
            $scope.table.trackScope(rowScope);
            rowScope.data = cellData;
            var content = $compile("<grid-status-distribution token-group='data' />")(rowScope);
            angular.element(td).html(content);
            //rowScope.$apply();
          }
        } 
        ];

  	} ])

.controller('TokenListCtrl', [
	'$scope',
	'$interval',
	'$http',
	function($scope, $interval, $http) {
	  $scope.$state = 'tokens'
	  
	  $scope.tokens;  
	    
	  $scope.loadTable = function loadTable() {
      $http.get("rest/grid/token").then(function(response) {
        $scope.tokens = []
        _.each(response.data, function(e) {
          var type = e.token.attributes.$agenttype;
          $scope.tokens.push({
            id : e.token.id,
            typeLabel : type == 'default' ? 'Java' : (type == 'node' ? 'Node.js' : (type == 'dotnet' ? '.NET' : 'Unknown')),
            type : type,
            attributes : e.token.attributes,
            attributesAsString : JSON.stringify(e.token.attributes),
            agentUrl : e.agent.agentUrl,
            executionId : e.currentOwner ? e.currentOwner.executionId : null,
            executionDescription : e.currentOwner ? e.currentOwner.executionDescription : null,
            status : (e.tokenHealth.hasError ? 'ERROR' : (e.currentOwner ? 'IN_USE' : 'FREE')),
            errorMessage : e.tokenHealth.errorMessage,
            hasError : e.tokenHealth.hasError
          });
        })
      });
	  };
	  
	  $scope.removeTokenError = function(tokenId) {
	    $http.post("rest/grid/token/"+tokenId+"/repair").then(function(){$scope.loadTable()});
	  }

	  $scope.loadTable();
      var refreshTimer = $interval(function(){
        if($scope.autorefresh){$scope.loadTable()}}, 5000);
      
      $scope.$on('$destroy', function() {
        $interval.cancel(refreshTimer);
      });
	} ])
	
.controller('QuotaManagerCtrl', [
    '$scope',
    '$http',
    '$interval',
    function($scope, $http, $interval) {
      $scope.$state = 'quotamanager'
      
      $scope.load = function loadTable() {
        $http.get("rest/quotamanager/status").then(
            function(response) {
              $scope.statusText = response.data;
            },function(error){
              $scope.statusText = "";
            });
      };
      
      var refreshTimer = $interval(function(){
        if($scope.autorefresh){$scope.load();}}, 2000);
      
      $scope.$on('$destroy', function() {
        $interval.cancel(refreshTimer);
      });
      
      $scope.load();

    } ]);
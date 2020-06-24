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
angular.module('planEditor',['step','artefacts','reportTable','dynamicForms','export'])

.run(function(ViewRegistry, EntityRegistry, PlanTypeRegistry) {  
  PlanTypeRegistry.register('step.core.plans.Plan', 'Default', 'partials/plans/planTreeEditor.html');
})

.controller('PlanEditorCtrl', function($scope, $compile, $http, stateStorage, $interval, $uibModal, $location,Dialogs, PlanTypeRegistry, AuthService, reportTableFactory, executionServices, ExportDialogs) {
  $scope.authService = AuthService;
  stateStorage.push($scope, 'editor', {});
      
  $scope.model = {}
  
  $scope.$watch('$state',function() {
    if($scope.$state!=null) {
      loadPlan($scope.$state);
    }
  });
      
  function loadPlan(id) {
    $scope.planId = id;
    $http.get('rest/plans/'+id).then(function(response){
      $scope.model.plan = response.data
    })
  }

  function savePlan(plan) {
    return $http.post("rest/plans", plan);
  }
  
  $scope.save = function() {
    savePlan($scope.model.plan);
  }

  $scope.exportPlan = function() {
    ExportDialogs.displayExportDialog('Plans export','plan/'+$scope.planId, $scope.model.plan.attributes.name+'.json', true).then(function () {})
  }
  
  $scope.clonePlan = function() {
    modalResult = Dialogs.enterValue('Clone plan as ',$scope.model.plan.attributes.name+'_Copy', 'md', 'enterValueDialog', function(value) {
      $http.get("rest/plans/"+$scope.planId+"/clone").then(function(response){
        var clonePlan = response.data;
        clonePlan.attributes.name = value;
        savePlan(clonePlan).then(function() {
          $location.path('/root/plans/editor/' + clonePlan.id);
        });
      })
    });
  }
  
  $scope.getEditorView = function() {
    if($scope.model.plan) {
      return PlanTypeRegistry.getEditorView($scope.model.plan._class)
    } else {
      return null;
    }
  }

  // ------------------------------------
  // Component tables
  //--------------------------------------
  $scope.componentTabs = {selectedTab:0};
  $scope.handle = {};

  // Controls
  $scope.addControl = function(id) {
    $scope.handle.addControl(id);
  }
      
  // Keywords
  $scope.addFunction = function(id) {
    $scope.handle.addFunction(id);
  }
  
  // Other plans
  $scope.addPlan = function(id) {
    $scope.handle.addPlan(id);
  }
       
  //------------------------------------
  // Interactive functions
  //------------------------------------
  $scope.artefactRef = function() {return {repositoryID:'local',repositoryParameters:{planid:$scope.planId}}};

  $scope.executionParameters = {};
  executionServices.getDefaultExecutionParameters().then(function(data){
    $scope.executionParameters = data;
  })
  
  $scope.interactiveSession = {
      execute: function(artefact) {
        var parameters = {executionParameters:$scope.executionParameters}
        var sessionId = $scope.interactiveSession.id;
        $scope.componentTabs.selectedTab = 3;
        $http.post("rest/interactive/"+sessionId+"/execute/"+$scope.model.plan.id+"/"+artefact.id, parameters).then(function() {
          $scope.stepsTable.reload();
        });
      },
      start: function() {
        $scope.startInteractive();
      }
  };
     
  $scope.isInteractiveSessionActive = function() {
    return $scope.interactiveSession.id != null;
  }
  
  $scope.startInteractive = function() {
    $http.post("rest/interactive/start").then(function(response){
      var interactiveSessionId = response.data;
      $scope.interactiveSession.id = interactiveSessionId;
    })
  }
  
  $scope.resetInteractive = function() {
    $scope.stopInteractive();
    $scope.startInteractive();
        
  }
  
  $scope.stopInteractive = function() {
    $http.post("rest/interactive/"+$scope.interactiveSession.id+"/stop").then()
    $scope.interactiveSession.id = null;
    //need to reset the context of the console table
    $scope.stepsTable = reportTableFactory.get(function() {
      return {'eid':$scope.interactiveSession.id};     
    }, $scope);
  }
  
  $scope.$on('$destroy', function() {
    if($scope.interactiveSession.id) {
      $scope.stopInteractive();
    }
  });

  $scope.stepsTable = reportTableFactory.get(function() {
    return {'eid':$scope.interactiveSession.id};     
  }, $scope);
})
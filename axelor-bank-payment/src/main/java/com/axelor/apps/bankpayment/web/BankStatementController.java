/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.bankpayment.web;

import com.axelor.apps.bankpayment.db.BankStatement;
import com.axelor.apps.bankpayment.db.repo.BankStatementRepository;
import com.axelor.apps.bankpayment.service.bankstatement.BankStatementService;
import com.axelor.exception.service.TraceBackService;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class BankStatementController {
	
	
	@Inject
	protected BankStatementService bankStatementService;
	
	@Inject
	protected BankStatementRepository bankStatementRepository;
	
	
	public void runImport(ActionRequest request, ActionResponse response ) {

		try {
			BankStatement bankStatement = request.getContext().asType(BankStatement.class);
			bankStatement = bankStatementRepository.find(bankStatement.getId());
			bankStatementService.runImport(bankStatement, true);
			
		} catch (Exception e) {
			TraceBackService.trace(response, e);
		}
		response.setReload(true);
	}
	
    public void print(ActionRequest request, ActionResponse response ) {
        try {
            BankStatement bankStatement = request.getContext().asType(BankStatement.class);
            bankStatement = bankStatementRepository.find(bankStatement.getId());
            String name = bankStatement.getName();
            String fileLink = bankStatementService.print(bankStatement);
            response.setView(ActionView
                    .define(name)
                    .add("html", fileLink).map());

        } catch (Exception e) {
            TraceBackService.trace(response, e);
        }

        response.setReload(true);
    }

}

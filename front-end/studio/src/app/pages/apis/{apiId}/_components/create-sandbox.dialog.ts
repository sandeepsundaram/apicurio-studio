/**
 * @license
 * Copyright 2020 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, QueryList, ViewChildren, Input} from "@angular/core";
import {ModalDirective} from "ngx-bootstrap/modal";
import {ConfigService} from "../../../../services/config.service";
import {Api} from "../../../../models/api.model";
import {DropDownOption, DropDownOptionValue} from "../../../../components/common/drop-down.component";


@Component({
    selector: "create-sandbox-dialog",
    templateUrl: "create-sandbox.dialog.html",
    styleUrls: [ "create-sandbox.dialog.css" ]
})
export class CreateSandboxModalComponent {

    @ViewChildren("createSandboxModal") createSandboxModal: QueryList<ModalDirective>;

    protected _isOpen: boolean = false;
    api: Api;
    uiUrl: string = "";
    format: string = "json";
    formats: DropDownOption[];
    dereference: string;
    refOptions: DropDownOption[] = [
        new DropDownOptionValue("Download API Spec As-Is", "false"),
        new DropDownOptionValue("Dereference All External $refs", "true")
    ];

    constructor(private config: ConfigService) {
        if (this.config.uiUrl()) {
            this.uiUrl = this.config.uiUrl();
        }
    }

    /**
     * Called to open the dialog.
     */
    public open(api: Api): void {
        this.api = api;
        this._isOpen = true;
        this.createSandboxModal.changes.subscribe( thing => {
            if (this.createSandboxModal.first) {
                this.createSandboxModal.first.show();
            }
        });
    }

    /**
     * Called to close the dialog.
     */
    public close(): void {
        this._isOpen = false;
    }

    /**
     * Returns true if the dialog is open.
     */
    public isOpen(): boolean {
        return this._isOpen;
    }

    /**
     * Copies the URL to the clipboard.
     */
    public downloadLink(): string {
        console.log(JSON.stringify(this.api));
        return `${this.config.uiUrl()}sandbox?session=${this.config.authToken()}&application=${this.api.name}&version=${this.api.type}&aid=${this.api.id}`;        
    }

}

/**
 * @license
 * Copyright 2017 JBoss Inc
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

import {IAuthenticationService} from "./auth.service";
import {User} from "../models/user.model";
import {ConfigService} from "./config.service";
import {HttpClient} from "@angular/common/http";
import {Topic} from "apicurio-ts-core";
import {ApicurioRole} from "../models/apicurio-role.enum";

/**
 * A version of the authentication service that uses keycloak-js to provide
 * authentication services.
 */
export class KeycloakAuthenticationService extends IAuthenticationService {

    private _authenticated: Topic<boolean> = new Topic<boolean>();
    private _user: User;

    private keycloak: any;

    /**
     * Constructor.
     * @param http
     * @param config
     */
    constructor(private http: HttpClient, private config: ConfigService) {
        super();
        let w: any = window;
        this.keycloak = w["keycloak"];

        let user: User = new User();
        user.name = "name1"
        user.login = "s@s.com"
        user.email = "s@ss.com"
        user.roles = ["admin"]

        this._authenticated.send(true);
        this._user = user;

    }

    /**
     * Returns the observable for is/isnot authenticated.
     * 
     */
    public isAuthenticated(): boolean {
        return true
    }

    /**
     * Returns the currently authenticated user.
     * 
     */
    public getAuthenticatedUserNow(): User {
        return this._user;
    }

    /**
     * Returns the topic to listen for auth changes.
     */
    public authenticated(): Topic<boolean> {
        return this._authenticated;
    }

    /**
     * Not supported.
     * @param user
     * @param credential
     */
    public login(user: string, credential: any): Promise<User> {
        throw new Error("Not supported.");
    }

    /**
     * Logout.
     */
    public logout(): void {
//         this.keycloak.logout({ redirectUri: location.href });
    }

    /**
     * Called to inject authentication headers into a remote API call.
     * @param headers
     */
    public injectAuthHeaders(headers: {[header: string]: string}): void {
        let authHeader: string = "bearer " + this.keycloak.token;
        headers["Authorization"] = authHeader;
    }

    /**
     * Called to return the keycloak access token.
     * 
     */
    public getAuthenticationSecret(): string {
        return "wwww"
    }

}
